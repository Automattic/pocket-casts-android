import json
import subprocess
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
EXPORTER = REPO_ROOT / "scripts" / "export_functiongemma_benchmark_fixture.py"


class ExportFunctionGemmaBenchmarkFixtureTest(unittest.TestCase):
    def test_exports_history_transcript_and_expected_call(self):
        sample = (
            "<start_of_turn>developer\nignored<end_of_turn>\n"
            "<start_of_turn>user\nRename the bookmark.<end_of_turn>\n"
            "<start_of_turn>model\n"
            "<start_function_call>call:dialog_control{action:<escape>begin</escape>,"
            "target_tool:<escape>bookmark</escape>,target_action:<escape>rename</escape>}"
            "<end_function_call><end_of_turn>\n"
            "<start_of_turn>user\nThe second one.<end_of_turn>\n"
            "<start_of_turn>model\n"
            "<start_function_call>call:dialog_control{action:<escape>provide_slot</escape>,"
            "target_tool:<escape>bookmark</escape>,target_action:<escape>rename</escape>,"
            "slot:<escape>ref</escape>,value:<escape>2</escape>}<end_function_call>"
            "<end_of_turn>"
        )

        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "valid.jsonl"
            destination = Path(directory) / "fixture.json"
            source.write_text(json.dumps({"text": sample}) + "\n")

            result = subprocess.run(
                [
                    "python3",
                    str(EXPORTER),
                    "--source",
                    str(source),
                    "--output",
                    str(destination),
                    "--count",
                    "1",
                ],
                capture_output=True,
                check=False,
                text=True,
            )

            self.assertEqual("", result.stderr)
            self.assertEqual(0, result.returncode)
            self.assertEqual(
                [
                    {
                        "history": [
                            {"role": "user", "content": "Rename the bookmark."},
                            {
                                "role": "model",
                                "content": (
                                    "<start_function_call>call:dialog_control{"
                                    "action:<escape>begin</escape>,"
                                    "target_tool:<escape>bookmark</escape>,"
                                    "target_action:<escape>rename</escape>}"
                                    "<end_function_call>"
                                ),
                            },
                        ],
                        "transcript": "The second one.",
                        "expected_name": "dialog_control",
                        "expected_action": "provide_slot",
                        "expected_params": {
                            "target_tool": "bookmark",
                            "target_action": "rename",
                            "slot": "ref",
                            "value": "2",
                        },
                    },
                ],
                json.loads(destination.read_text()),
            )


if __name__ == "__main__":
    unittest.main()

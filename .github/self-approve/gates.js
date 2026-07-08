// Objective FACT prechecks for the self-approve evaluator.
//
// Loaded by .github/workflows/self-approve.yml via actions/github-script.
// This module answers only objective, boolean questions: is the author
// allowlisted, did CI pass, did the AI review run, are its threads resolved.
//
// The judgment gates (critical areas touched, low risk) are deliberately NOT
// here. Those are made by Claude reading the diff, with the criteria defined in
// the workflow's prompt, so nothing about "is this change risky?" is hardcoded
// as JavaScript logic.
//
// It reads PR metadata through the API only and never executes PR-authored code,
// so it is safe to run from pull_request_target.

const SELF_APPROVERS = (process.env.SELF_APPROVERS || "geekygecko sztomek")
  .split(/\s+/)
  .filter(Boolean);

const LABEL = "[Review] Self Approved";
const COMMENT_MARKER = "<!-- self-approve-eval -->";

// The AI review's check-run name (the Claude Code Review workflow's job).
const AI_REVIEW_CHECK = "claude-review";
// The login that authors the AI review threads (GraphQL author login).
const AI_THREAD_LOGIN = "claude";

// The required status checks on main. Kept in sync with branch protection by
// hand: GITHUB_TOKEN cannot read branch protection, so we cannot fetch these at
// runtime. If the required checks change, update this list.
const REQUIRED_CHECKS = [
  "buildkite/pocket-casts-android/lint",
  "buildkite/pocket-casts-android/unit-tests",
  "buildkite/pocket-casts-android/instrumented-tests",
  "buildkite/pocket-casts-android/assemble-app-release-apk",
  "buildkite/pocket-casts-android/assemble-automotive-release-apk",
  "buildkite/pocket-casts-android/assemble-wear-release-apk",
  "danger/pr-check",
  "license/cla",
];

async function readChecks(github, owner, repo, sha) {
  const state = new Map();
  const combined = await github.rest.repos.getCombinedStatusForRef({
    owner,
    repo,
    ref: sha,
  });
  for (const status of combined.data.statuses) {
    state.set(status.context, status.state); // success | pending | failure | error
  }
  const runs = await github.paginate(github.rest.checks.listForRef, {
    owner,
    repo,
    ref: sha,
    per_page: 100,
  });
  for (const run of runs) {
    state.set(run.name, run.status === "completed" ? run.conclusion : "pending");
  }
  return state;
}

function fact(key, name, status, reason) {
  return { key, name, status, reason };
}

// Checks the objective prerequisites for a PR. Fetches fresh by number so it
// behaves identically for every triggering event.
async function checkFacts({ github, context, prNumber }) {
  const { owner, repo } = context.repo;

  const { data: pr } = await github.rest.pulls.get({
    owner,
    repo,
    pull_number: prNumber,
  });
  const author = pr.user.login;
  const headSha = pr.head.sha;

  const result = {
    prNumber,
    author,
    headSha,
    baseRef: pr.base.ref,
    allowlisted: SELF_APPROVERS.includes(author),
    facts: [],
    factsPassed: false,
  };

  if (pr.state !== "open" || pr.draft) {
    result.blocked = pr.draft ? "PR is a draft" : `PR is ${pr.state}`;
    return result;
  }

  const checkState = await readChecks(github, owner, repo, headSha);

  // Fact: an AI review completed for the current commit.
  const aiReview = checkState.get(AI_REVIEW_CHECK);
  result.facts.push(
    aiReview === "success"
      ? fact("ai_review", "AI review completed", "pass", `${AI_REVIEW_CHECK} succeeded on ${headSha.slice(0, 7)}`)
      : aiReview === undefined || aiReview === "pending"
        ? fact("ai_review", "AI review completed", "pending", "waiting for the Claude review on the latest commit")
        : fact("ai_review", "AI review completed", "fail", `Claude review did not run on the latest commit (${aiReview})`),
  );

  // Fact: every AI review thread is resolved.
  const threadData = await github.graphql(
    `query($owner: String!, $repo: String!, $number: Int!) {
       repository(owner: $owner, name: $repo) {
         pullRequest(number: $number) {
           reviewThreads(first: 100) {
             nodes { isResolved comments(first: 1) { nodes { author { login } } } }
           }
         }
       }
     }`,
    { owner, repo, number: prNumber },
  );
  const aiThreads = threadData.repository.pullRequest.reviewThreads.nodes.filter(
    (thread) => thread.comments.nodes[0]?.author?.login === AI_THREAD_LOGIN,
  );
  const unresolved = aiThreads.filter((thread) => !thread.isResolved).length;
  result.facts.push(
    unresolved === 0
      ? fact("ai_threads", "AI comments resolved", "pass", `${aiThreads.length} AI thread(s), all resolved`)
      : fact("ai_threads", "AI comments resolved", "fail", `${unresolved} unresolved AI review thread(s)`),
  );

  // Fact: required CI checks are green on the current commit.
  const missing = [];
  const pending = [];
  const failing = [];
  for (const check of REQUIRED_CHECKS) {
    const status = checkState.get(check);
    if (status === undefined) missing.push(check);
    else if (status === "success" || status === "skipped" || status === "neutral") continue;
    else if (status === "pending") pending.push(check);
    else failing.push(`${check} (${status})`);
  }
  if (failing.length > 0)
    result.facts.push(fact("ci", "CI checks passed", "fail", `failing: ${failing.join(", ")}`));
  else if (pending.length > 0 || missing.length > 0)
    result.facts.push(fact("ci", "CI checks passed", "pending", `still running: ${[...pending, ...missing].join(", ")}`));
  else result.facts.push(fact("ci", "CI checks passed", "pass", "all required checks green"));

  result.factsPassed =
    result.allowlisted && result.facts.every((entry) => entry.status === "pass");
  return result;
}

const ICON = { pass: "✅", fail: "❌", pending: "⏳" };

// The comment shown while the objective prerequisites are not yet met. When they
// all pass, this module stays silent and Claude writes the verdict comment.
function renderFactsPendingComment(result) {
  const lines = [
    COMMENT_MARKER,
    "## Self-approval eligibility",
    "",
    "Objective prerequisites are not all met yet:",
    "",
    "| | Check | Detail |",
    "|---|---|---|",
  ];
  for (const entry of result.facts) {
    lines.push(`| ${ICON[entry.status]} | ${entry.name} | ${entry.reason} |`);
  }
  lines.push(
    "",
    "Once these are green the risk assessment runs automatically, or comment `/self-approve` to re-check.",
    "",
    `<sub>Checked against \`${result.headSha.slice(0, 7)}\`.</sub>`,
  );
  return lines.join("\n");
}

// Upserts the single sticky comment, editing in place so repeated evaluations
// never spam the PR timeline. Claude reuses the same marker for the verdict.
async function upsertComment({ github, context, prNumber, body }) {
  const { owner, repo } = context.repo;
  const comments = await github.paginate(github.rest.issues.listComments, {
    owner,
    repo,
    issue_number: prNumber,
    per_page: 100,
  });
  const existing = comments.find((comment) => comment.body?.includes(COMMENT_MARKER));
  if (existing) {
    await github.rest.issues.updateComment({ owner, repo, comment_id: existing.id, body });
  } else {
    await github.rest.issues.createComment({ owner, repo, issue_number: prNumber, body });
  }
}

module.exports = {
  SELF_APPROVERS,
  LABEL,
  COMMENT_MARKER,
  checkFacts,
  renderFactsPendingComment,
  upsertComment,
};

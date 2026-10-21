# What's New mock catalog

`mock-catalog.json` has one message for every page action the admin page can publish: `create_playlist`, `open_link`, `open_networks`, `open_discover`, `open_podcasts`, `open_up_next`, `open_playlists`, `open_profile` and `open_settings`.

To show it in a debug build, seed it into the app's HTTP cache:

```bash
scripts/whats_new_mock_catalog.py
```

The script stops the app, then stores the file as the cached response for `https://static.pocketcasts.net/whats-new/v1/android/en.json`. Open the app and go to Profile → What's new.

Options:

- Pass a different catalog file as the first argument.
- `-s <serial>` picks a device.
- `--package`, `--host` and `--locale` target another build or locale. For `debugProd`, use `--package au.com.shiftyjelly.pocketcasts --host static.pocketcasts.com`.
- `--reset` removes the seeded response.

Pulling to refresh the feed fetches the real catalog again and replaces the mock. Run the script again to bring the mock back.

@AGENTS.md

## Upstream Merge Policy

When updating code from upstream into origin:

1. **Always preserve origin's functionality** - Changes in origin take precedence over upstream changes when there are conflicts
2. **Before merging upstream**:
   - Identify all files modified in origin that differ from upstream
   - Document the purpose of each origin modification
3. **During merge conflicts**:
   - Keep origin's implementation when functionality would be lost
   - If upstream has important updates, integrate them while ensuring origin's custom behavior remains intact
4. **After merging**:
   - Verify all origin-specific functionality still works
   - Run tests to confirm no regressions
   - If upstream changes break origin functionality, revert or adapt the upstream changes to preserve origin's behavior

**Priority order**: Origin functionality > Upstream updates

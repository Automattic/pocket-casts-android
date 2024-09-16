#!/bin/bash -u

echo "--- :memo: Commenting on the PR with the dependency diff"
comment_with_dependency_diff 'app' 'releaseRuntimeClasspath'

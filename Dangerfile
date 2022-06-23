# Sometimes it's a README fix, or something like that - which isn't relevant for
# including in a project's CHANGELOG for example
declared_trivial = github.pr_title.include? "#trivial"

# Make it more obvious that a PR is a work in progress and shouldn't be merged yet
warn("PR is classed as Work in Progress") if github.pr_title.include? "[WIP]"

# Don't run danger when merging develop to master
if github.branch_for_head != "develop" && github.branch_for_base != "master"
    # Error when there are new lint errors
    android_lint.filtering = true
    android_lint.skip_gradle_task = true
    Dir.glob("**/build/reports/lint-results.xml").each { |file| 
        puts "Checking " + file
        android_lint.report_file = file
        android_lint.lint(inline_mode: false)
    }

    # Display test results
    Dir.glob("**/**/build/test-results/test*UnitTest/*.xml").each { |file| 
        puts "Checking " + file
        junit.parse file
        junit.report
    }

    # Display UI test results
    Dir.glob("**/**/build/outputs/androidTest-results/connected/*.xml").each { |file| 
        puts "Checking " + file
        junit.parse file
        junit.report
    }
end
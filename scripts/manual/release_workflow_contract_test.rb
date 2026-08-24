#!/usr/bin/env ruby
# frozen_string_literal: true

require "minitest/autorun"

class ReleaseWorkflowContractTest < Minitest::Test
  WORKFLOW_PATH = File.expand_path("../../.github/workflows/release.yml", __dir__)

  def setup
    @workflow = File.read(WORKFLOW_PATH)
  end

  def test_legacy_release_tag_validation_is_explicit_and_fail_closed
    assert_includes @workflow, 'elif [[ "$MANUAL_VERSION" == "0.3.0" ]]'
    assert_includes @workflow, "scripts/manual/release_inventory.rb"
    assert_includes @workflow, "scripts/manual/validate_release_manuals.rb"
    assert_includes @workflow, "No supported manual validation contract"
  end

  def test_current_manual_uses_the_stable_version_for_prerelease_tags
    assert_includes @workflow, 'MANUAL_VERSION="${VERSION%%-*}"'
    assert_includes @workflow, 'current_manual_contract.rb --version "$MANUAL_VERSION"'
  end

  def test_release_notes_require_an_exact_changelog_section
    assert_includes @workflow, 'CHANGELOG.md must contain an exact section for [$VERSION]'
    refute_match(/Using fallback notes/, @workflow)
  end

  def test_any_valid_prerelease_suffix_is_published_as_prerelease
    assert_includes @workflow, '[[ "$VERSION" =~ -[A-Za-z0-9.]+$ ]]'
  end
end

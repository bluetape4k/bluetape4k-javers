require "minitest/autorun"

class BomReadmeVersionTest < Minitest::Test
  ROOT = File.expand_path("../..", __dir__)
  README_PATHS = [
    File.join(ROOT, "javers-spring-boot4-autoconfigure", "README.md"),
    File.join(ROOT, "javers-spring-boot4-autoconfigure", "README.ko.md"),
  ].freeze

  def test_autoconfigure_readmes_use_the_current_stable_bom
    expected_version = current_stable_version

    README_PATHS.each do |path|
      content = File.read(path)
      versions = content.scan(/bluetape4k-javers-bom:([0-9]+\.[0-9]+\.[0-9]+)/).flatten

      assert_equal [expected_version], versions, "stale or missing BOM version in #{path}"
    end
  end

  private

  def current_stable_version
    content = File.read(File.join(ROOT, "README.md"))
    match = content.match(/^Current stable version: `([^`]+)`$/)
    refute_nil match, "root README must declare the current stable version"
    match[1]
  end
end

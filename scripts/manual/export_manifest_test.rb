require "json"
require "minitest/autorun"
require "tmpdir"

require_relative "export_manifest"

class ExportManifestTest < Minitest::Test
  def test_sorts_keys_and_inventory_rows_deterministically
    Dir.mktmpdir do |root|
      source = File.join(root, "manifest.yaml")
      output = File.join(root, "manifest.json")
      File.write(source, <<~YAML)
        modules:
          - id: zeta
            kind: example
          - kind: library
            id: alpha
        releaseRef: 0.3.0
        schemaVersion: 2
      YAML
      exporter = ManualDocs::ManifestExporter.new(source_path: source, output_path: output)
      exporter.write
      first = File.binread(output)
      exporter.write

      assert_equal first, File.binread(output)
      parsed = JSON.parse(first)
      assert_equal %w[modules releaseRef schemaVersion], parsed.keys
      assert_equal %w[alpha zeta], parsed.fetch("modules").map { |row| row.fetch("id") }
      assert first.end_with?("\n")
      assert exporter.current?
    end
  end
end

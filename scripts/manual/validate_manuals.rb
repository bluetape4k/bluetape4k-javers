#!/usr/bin/env ruby

require "json"
require_relative "manual_contract"

inventory_path = ARGV.fetch(0, "build/manual/release-module-inventory.json")
expected_release = {
  "ref" => ENV.fetch("MANUAL_RELEASE_REF", "0.3.0"),
  "commit" => ENV.fetch("MANUAL_RELEASE_COMMIT", "978d0490fc438570e7520643aed50e20614772d1"),
}
validator = ManualDocs::Validator.new(
  inventory: JSON.parse(File.read(inventory_path)),
  manifest_path: "docs/manual/manifest.yaml",
  repository_root: Dir.pwd,
  expected_release: expected_release,
  strict: ENV["MANUAL_STRICT"] == "1",
)
abort(validator.errors.join("\n")) unless validator.errors.empty?
mode = ENV["MANUAL_STRICT"] == "1" ? "strict release mode" : "incremental authoring mode"
puts "Manual contract valid (#{mode})."

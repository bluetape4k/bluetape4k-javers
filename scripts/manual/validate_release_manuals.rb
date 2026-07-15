#!/usr/bin/env ruby

require "json"
require "yaml"
require_relative "manual_contract"
require_relative "release_contract"

module ManualDocs
  class ReleaseManualValidator
    def initialize(repository_root:, tag:, expected_sha:, inventory_path:, manifest_path:, release_contract: nil)
      @repository_root = File.expand_path(repository_root)
      @tag = tag
      @expected_sha = expected_sha
      @inventory_path = File.expand_path(inventory_path, @repository_root)
      @manifest_path = File.expand_path(manifest_path, @repository_root)
      @release_contract = release_contract || ReleaseContract.new(
        repository_root: @repository_root, tag: @tag, expected_sha: @expected_sha, manifest_path: @manifest_path,
      )
      @release_result = nil
    end

    def errors
      release_result = result
      errors = release_result.errors.dup
      return errors << "release inventory not found: #{@inventory_path}" unless File.file?(@inventory_path)
      return errors << "manual manifest not found: #{@manifest_path}" unless File.file?(@manifest_path)
      manifest = YAML.safe_load(File.read(@manifest_path))
      unless manifest.is_a?(Hash) && manifest.dig("publication", "contentStatus") == "complete"
        errors << "final release manual publication contentStatus must be complete"
      end
      if manifest.is_a?(Hash) && manifest.dig("publication", "contentStatus") == "complete"
        documents = manifest.dig("overview", "documents")
        modules = manifest["modules"]
        ManualDocs::Validator::LOCALES.keys.each do |locale|
          registered = Array(documents.is_a?(Hash) ? documents[locale] : nil) +
            Array(modules).grep(Hash).map { |entry| entry[locale] }.compact
          errors << "final release manual must register non-empty #{locale} routes" if registered.empty?
        end
      end
      inventory = JSON.parse(File.read(@inventory_path))
      errors.concat(Validator.new(
        inventory: inventory, manifest_path: @manifest_path, repository_root: @repository_root,
        expected_release: { "ref" => @tag, "commit" => @expected_sha }, strict: true,
      ).errors)
      errors.sort
    rescue JSON::ParserError => error
      errors << "release inventory JSON is invalid: #{error.message}"
    rescue Psych::SyntaxError => error
      errors << "manual manifest YAML is invalid: #{error.problem}"
    end

    def checked_link_count
      result.checked_count
    end

    def checked_source_path_count
      result.source_path_count || 0
    end

    def checked_evidence_path_count
      result.evidence_path_count || 0
    end

    private

    def result
      @release_result ||= @release_contract.validate
    end
  end
end

if $PROGRAM_NAME == __FILE__
  tag = ARGV.fetch(0, "0.2.1")
  sha = ARGV.fetch(1, "bffe19439ca891fa5301a76421bdef7ba75252a0")
  inventory_path = ARGV.fetch(2, "build/manual/release-module-inventory.json")
  validator = ManualDocs::ReleaseManualValidator.new(
    repository_root: Dir.pwd, tag: tag, expected_sha: sha,
    inventory_path: inventory_path, manifest_path: "docs/manual/manifest.yaml",
  )
  errors = validator.errors
  abort(errors.join("\n")) unless errors.empty?
  puts "Strict release manual contract valid: annotated tag #{tag} -> #{sha}; " \
       "#{validator.checked_source_path_count} source paths, #{validator.checked_evidence_path_count} benchmark evidence file, " \
       "and #{validator.checked_link_count} release-local links checked."
end

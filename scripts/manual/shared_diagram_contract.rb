# frozen_string_literal: true

require "digest"
require "fileutils"
require "open3"
require "pathname"
require "yaml"

module SharedDiagrams
  class ContractError < StandardError; end

  Entry = Struct.new(:id, :kind, :canonical, :manual, :manual_pages, :source_paths, keyword_init: true) do
    def selected?
      manual == "selected"
    end

    def deferred?
      manual == "deferred"
    end
  end

  class Contract
    CANONICAL_ROOT = "docs/images/readme-diagrams"
    MIRROR_ROOT = "docs/manual/assets/readme-diagrams"
    MANUAL_MANIFEST = "docs/manual/manifest.yaml"
    VALID_KINDS = %w[architecture class erd flow sequence].freeze
    VALID_MANUAL_STATES = %w[selected deferred].freeze

    attr_reader :root, :inventory_path

    def initialize(root:, inventory_path:)
      @root = Pathname.new(root).expand_path
      @inventory_path = Pathname.new(inventory_path).expand_path
    end

    def entries
      @entries ||= load_entries
    end

    def active_entries
      entries.select(&:selected?)
    end

    def release_ref
      value = manual_manifest["releaseRef"]
      unless non_empty_string?(value) && !value.start_with?("-") && !value.include?("..") && value.match?(/\A[0-9A-Za-z][0-9A-Za-z._\/-]*\z/)
        raise ContractError, "manual manifest releaseRef is invalid"
      end

      value
    end

    def release_commit
      value = manual_manifest["releaseCommit"]
      raise ContractError, "manual manifest releaseCommit must be a full SHA" unless value.is_a?(String) && value.match?(/\A[0-9a-f]{40}\z/)

      value
    end

    def errors
      entry_list = entries
      failures = canonical_errors(entry_list) + reference_errors(entry_list)
      release_failures = release_provenance_errors
      release_failures += release_entry_errors(active_entries) if release_failures.empty?
      failures + release_failures + (release_failures.empty? ? mirror_errors(entry_list) : [])
    rescue ContractError => error
      [error.message]
    end

    def sync!
      entry_list = entries
      blockers = canonical_errors(entry_list) + reference_errors(entry_list) + release_provenance_errors
      blockers += release_entry_errors(active_entries) if blockers.empty?
      raise ContractError, blockers.join("\n") unless blockers.empty?

      mirror_root.mkpath
      expected = []
      active_entries.each do |entry|
        %w[svg png].each do |extension|
          target = mirror_path(entry, extension)
          File.binwrite(target, release_asset(entry, extension))
          expected << target.basename.to_s
        end
      end
      mirror_root.children.select(&:file?).each do |path|
        path.delete unless expected.include?(path.basename.to_s)
      end

      failures = errors
      raise ContractError, failures.join("\n") unless failures.empty?

      true
    end

    private

    def load_entries
      data = inventory_data
      raise ContractError, "shared diagram schemaVersion must be 3" unless data["schemaVersion"] == 3
      raise ContractError, "shared diagram sourcePolicy must be manual-release" unless data["sourcePolicy"] == "manual-release"

      rows = data["diagrams"]
      raise ContractError, "shared diagram inventory diagrams must be an array" unless rows.is_a?(Array)

      parsed = rows.map { |row| parse_entry(row) }
      duplicate_ids = parsed.group_by(&:id).select { |_id, matches| matches.size > 1 }.keys
      raise ContractError, "duplicate diagram ids: #{duplicate_ids.sort.join(', ')}" unless duplicate_ids.empty?
      duplicate_canonical = parsed.group_by(&:canonical).select { |_id, matches| matches.size > 1 }.keys
      raise ContractError, "duplicate canonical diagrams: #{duplicate_canonical.sort.join(', ')}" unless duplicate_canonical.empty?

      parsed.freeze
    end

    def inventory_data
      @inventory_data ||= load_yaml(inventory_path, "shared diagram inventory")
    end

    def manual_manifest
      @manual_manifest ||= load_yaml(resolved(MANUAL_MANIFEST), "manual manifest")
    end

    def load_yaml(path, label)
      raise ContractError, "#{label} not found" unless path.file?

      data = YAML.safe_load(path.read, permitted_classes: [], aliases: false)
      raise ContractError, "#{label} must be a mapping" unless data.is_a?(Hash)

      data
    rescue Psych::SyntaxError => error
      raise ContractError, "#{label} YAML is invalid: #{error.problem}"
    end

    def parse_entry(row)
      raise ContractError, "diagram entry must be a mapping" unless row.is_a?(Hash)

      id = row["id"]
      kind = row["kind"]
      canonical = row["canonical"]
      manual = row["manual"]
      source_paths = row["sourcePaths"]
      manual_pages = row["manualPages"]

      raise ContractError, "diagram id must be a non-empty string" unless non_empty_string?(id)
      raise ContractError, "#{id}: invalid kind #{kind.inspect}" unless VALID_KINDS.include?(kind)
      unless non_empty_string?(canonical) && canonical.match?(/\A[a-z0-9][a-z0-9-]*\z/)
        raise ContractError, "#{id}: unsafe canonical path #{canonical.inspect}"
      end
      raise ContractError, "#{id}: invalid manual state #{manual.inspect}" unless VALID_MANUAL_STATES.include?(manual)
      unless source_paths.is_a?(Array) && !source_paths.empty? && source_paths.all? { |path| safe_relative?(path) }
        raise ContractError, "#{id}: sourcePaths must contain safe relative paths"
      end
      if manual == "selected"
        unless manual_pages.is_a?(Hash) && manual_pages.keys.sort == %w[en ko] && manual_pages.values.all? { |path| safe_relative?(path) }
          raise ContractError, "#{id}: selected diagram must define safe en/ko manualPages"
        end
      elsif !manual_pages.nil?
        raise ContractError, "#{id}: deferred diagram must not define manualPages"
      end

      Entry.new(
        id: id,
        kind: kind,
        canonical: canonical,
        manual: manual,
        manual_pages: manual_pages || {},
        source_paths: source_paths,
      )
    end

    def canonical_errors(entry_list)
      entry_list.flat_map do |entry|
        %w[svg png].each_with_object([]) do |extension, failures|
          failures << "#{entry.id}: missing canonical #{extension.upcase}" unless canonical_path(entry, extension).file?
        end
      end
    end

    def reference_errors(entry_list)
      entry_list.flat_map do |entry|
        source_failures = entry.source_paths.each_with_object([]) do |path, failures|
          failures << "#{entry.id}: missing source path #{path}" unless resolved(path).exist?
        end
        page_failures = entry.manual_pages.each_with_object([]) do |(locale, path), failures|
          failures << "#{entry.id}: missing #{locale} manual page #{path}" unless resolved(path).file?
        end
        source_failures + page_failures
      end
    end

    def release_provenance_errors
      actual = git_capture("rev-parse", "#{release_ref}^{commit}").strip
      actual == release_commit ? [] : ["manual releaseRef #{release_ref} resolves to #{actual}, expected #{release_commit}"]
    rescue ContractError => error
      [error.message]
    end

    def release_entry_errors(entry_list)
      entry_list.flat_map do |entry|
        paths = %w[svg png].map { |extension| canonical_relative_path(entry, extension) } + entry.source_paths
        paths.each_with_object([]) do |path, failures|
          failures << "#{entry.id}: missing release source #{release_ref}:#{path}" unless git_object_exists?(path)
        end
      end
    end

    def mirror_errors(entry_list)
      expected = []
      active = active_entries.map(&:id).to_h { |id| [id, true] }
      failures = entry_list.flat_map do |entry|
        %w[svg png].flat_map do |extension|
          mirror = mirror_path(entry, extension)
          if active[entry.id]
            expected << mirror.basename.to_s
            if !mirror.file?
              ["#{entry.id}: missing mirror #{extension.upcase}"]
            elsif Digest::SHA256.file(mirror).hexdigest != Digest::SHA256.hexdigest(release_asset(entry, extension))
              ["#{entry.id}: release and mirror #{extension.upcase} digests differ"]
            else
              []
            end
          elsif mirror.file?
            ["#{entry.id}: deferred diagram has mirror #{extension.upcase}"]
          else
            []
          end
        end
      end
      if mirror_root.directory?
        mirror_root.children.select(&:file?).each do |path|
          failures << "orphan mirror asset: #{path.basename}" unless expected.include?(path.basename.to_s)
        end
      end
      failures
    end

    def release_asset(entry, extension)
      git_capture("show", "#{release_ref}:#{canonical_relative_path(entry, extension)}")
    end

    def git_object_exists?(path)
      _stdout, _stderr, status = Open3.capture3("git", "-C", root.to_s, "cat-file", "-e", "#{release_ref}:#{path}")
      status.success?
    end

    def git_capture(*arguments)
      stdout, stderr, status = Open3.capture3("git", "-C", root.to_s, *arguments, binmode: true)
      raise ContractError, "git #{arguments.first} failed: #{stderr.strip}" unless status.success?

      stdout
    rescue Errno::ENOENT => error
      raise ContractError, "git executable not found: #{error.message}"
    end

    def canonical_relative_path(entry, extension)
      "#{CANONICAL_ROOT}/#{entry.canonical}.#{extension}"
    end

    def canonical_path(entry, extension)
      resolved(canonical_relative_path(entry, extension))
    end

    def mirror_path(entry, extension)
      resolved("#{MIRROR_ROOT}/#{entry.canonical}.#{extension}")
    end

    def mirror_root
      resolved(MIRROR_ROOT)
    end

    def resolved(path)
      candidate = root.join(path).expand_path
      raise ContractError, "unsafe path #{path.inspect}" unless candidate == root || candidate.to_s.start_with?("#{root}#{File::SEPARATOR}")

      candidate
    end

    def safe_relative?(path)
      return false unless non_empty_string?(path)

      pathname = Pathname.new(path)
      !pathname.absolute? && pathname.each_filename.none? { |part| part == ".." }
    end

    def non_empty_string?(value)
      value.is_a?(String) && !value.empty?
    end
  end
end

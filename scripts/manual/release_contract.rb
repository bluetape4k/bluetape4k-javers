require "open3"
require "pathname"
require "set"
require "yaml"

module ManualDocs
  class ReleaseContract
    ValidationResult = Struct.new(:errors, :checked_count, :source_path_count, :evidence_path_count, keyword_init: true)
    LINK_PATTERN = /!?\[[^\]]*\]\(\s*<?([^)\s>]+)>?(?:\s+["'][^)]*["'])?\s*\)/
    REFERENCE_PATTERN = /^[ \t]{0,3}\[[^\]]+\]:[ \t]*(?:<([^>\r\n]+)>|([^ \t\r\n]+))/
    HTML_LINK_PATTERN = /<(?:a|img)\b[^>]*(?:href|src)=["']([^"']+)["']/i
    AUTOLINK_PATTERN = /<((?:https?:\/\/)[^ >]+)>/i
    GITHUB_BLOB_PATTERN = %r{\A(?i:https?://github\.com/bluetape4k/bluetape4k-javers)/blob/([^/]+)/(.+)\z}
    RAW_GITHUB_PATTERN = %r{\A(?i:https?://raw\.githubusercontent\.com/bluetape4k/bluetape4k-javers)/([^/]+)/(.+)\z}

    def initialize(repository_root:, tag:, expected_sha:, manifest_path: nil, git_runner: nil)
      @repository_root = File.expand_path(repository_root)
      @tag = tag
      @expected_sha = expected_sha
      @manifest_path = manifest_path && File.expand_path(manifest_path, @repository_root)
      @git_runner = git_runner || method(:run_git)
    end

    def errors
      validate.errors
    end

    def validate
      type, found = @git_runner.call(["cat-file", "-t", "refs/tags/#{@tag}"])
      return result(["release tag not found: #{@tag}"]) unless found
      return result(["release tag must be annotated: #{@tag}"]) unless type.strip == "tag"
      sha, found = @git_runner.call(["rev-parse", "--verify", "refs/tags/#{@tag}^{commit}"])
      return result(["release tag not found: #{@tag}"]) unless found
      sha = sha.strip
      return result(["release tag #{@tag} resolves to #{sha}, expected #{@expected_sha}"]) unless sha.casecmp?(@expected_sha)
      output, found = @git_runner.call(["ls-tree", "-r", "--name-only", sha])
      return result(["release tree could not be read: #{sha}"]) unless found
      tree = output.lines(chomp: true).to_set

      links = repository_links
      errors = links.each_with_object([]) do |(file, line, target), found_errors|
        path = Pathname.new(File.dirname(file)).join(target.split(/[?#]/, 2).first).cleanpath.to_s
        if path == ".." || path.start_with?("../") || Pathname.new(path).absolute?
          found_errors << "#{file}:#{line}: unsafe release path: #{target}"
        elsif !tree.include?(path)
          found_errors << "#{file}:#{line}: release path not found: #{path}"
        end
      end
      github_errors, github_count = validate_github_release_links(tree)
      source_errors, source_count, evidence_errors, evidence_count = validate_manifest_paths(tree)
      errors.concat(github_errors).concat(source_errors).concat(evidence_errors)
      result(errors.sort, links.length + github_count, source_count, evidence_count)
    end

    private

    def repository_links
      stable_manual_documents.flat_map do |path|
        file = Pathname.new(path).relative_path_from(Pathname.new(@repository_root)).to_s
        content = File.read(path)
        extracted_links(content).each_with_object([]) do |(line, target), links|
          next unless repository_target?(file, target)
          links << [file, line, target]
        end
      end
    end

    def validate_github_release_links(tree)
      links = stable_manual_documents.flat_map do |path|
        file = Pathname.new(path).relative_path_from(Pathname.new(@repository_root)).to_s
        content = File.read(path)
        extracted_links(content).each_with_object([]) do |(line, target), found_links|
          source = source_coordinates(target)
          next unless source
          found_links << [file, line, source[0], source[1].split(/[?#]/, 2).first]
        end
      end
      errors = links.each_with_object([]) do |(file, line, ref, path), found_errors|
        found_errors << "#{file}:#{line}: source link commit #{ref}, expected #{@expected_sha}" unless ref.casecmp?(@expected_sha)
        found_errors << "#{file}:#{line}: release path not found: #{path}" unless tree.include?(path)
      end
      [errors, links.length]
    end

    # The 0.3.0 release contract intentionally excludes the current manual.
    # Its source tree is newer than the immutable release tree and is checked
    # by current_manual_contract.rb during the release workflow instead.
    def stable_manual_documents
      %w[en ko].flat_map { |locale| Dir.glob(File.join(@repository_root, "docs/manual/#{locale}/**/*.md")) }.sort
    end

    def validate_manifest_paths(tree)
      return [[], 0, [], 0] unless @manifest_path && File.file?(@manifest_path)
      manifest = YAML.safe_load(File.read(@manifest_path))
      return [["manual manifest must be a mapping"], 0, [], 0] unless manifest.is_a?(Hash)
      modules = manifest["modules"]
      return [["manual manifest modules must be an array"], 0, [], 0] unless modules.is_a?(Array)

      source_errors = []
      source_paths = modules.grep(Hash).flat_map do |entry|
        paths = entry["sourcePaths"]
        unless paths.is_a?(Array) && paths.all? { |path| path.is_a?(String) && !path.empty? }
          source_errors << "#{entry['id'] || 'module'}: sourcePaths must contain non-empty strings"
          next []
        end
        source_errors << "#{entry['id'] || 'module'}: sourcePaths must equal [sourceDir]" unless paths == [entry["sourceDir"]]
        paths.map do |path|
          unless release_tree_contains?(tree, path)
            source_errors << "#{entry['id']}: sourcePath not found in release tree: #{path}"
          end
          path
        end
      end

      evidence_errors = []
      evidence = manifest.fetch("evidence", [])
      unless evidence.is_a?(Array)
        evidence_errors << "manual evidence must be an array"
        evidence = []
      end
      evidence_paths = evidence.grep(Hash).each_with_object([]) do |entry, paths|
        path = entry["path"]
        unless path.is_a?(String) && !path.empty?
          evidence_errors << "#{entry['id'] || 'evidence'}: evidence path must be a non-empty string"
          next
        end
        evidence_errors << "#{entry['id']}: evidence path not found in release tree: #{path}" unless safe_relative?(path) && tree.include?(path)
        paths << path
      end
      [source_errors, source_paths.length, evidence_errors, evidence_paths.length]
    rescue Psych::SyntaxError => error
      [["manual manifest YAML is invalid: #{error.problem}"], 0, [], 0]
    end

    def release_tree_contains?(tree, path)
      safe_relative?(path) && tree.any? { |release_path| release_path == path || release_path.start_with?("#{path}/") }
    end

    def extracted_links(content)
      patterns = [LINK_PATTERN, REFERENCE_PATTERN, HTML_LINK_PATTERN, AUTOLINK_PATTERN]
      patterns.flat_map do |pattern|
        content.to_enum(:scan, pattern).map do
          match = Regexp.last_match
          [content[0...match.begin(0)].count("\n") + 1, match.captures.compact.first]
        end
      end.uniq
    end

    def source_coordinates(target)
      source = target.match(GITHUB_BLOB_PATTERN) || target.match(RAW_GITHUB_PATTERN)
      source && [source[1], source[2]]
    end

    def safe_relative?(value)
      value.is_a?(String) && !value.empty? && !Pathname.new(value).absolute? && Pathname.new(value).each_filename.none? { |part| part == ".." }
    end

    def repository_target?(file, target)
      return false if target.empty? || target.start_with?("#", "/") || target.match?(/\A[a-z][a-z0-9+.-]*:/i)
      candidate = Pathname.new(File.dirname(file)).join(target.split(/[?#]/, 2).first).cleanpath.to_s
      candidate != "docs/manual" && !candidate.start_with?("docs/manual/")
    end

    def result(errors, count = 0, source_path_count = 0, evidence_path_count = 0)
      ValidationResult.new(
        errors: errors, checked_count: count, source_path_count: source_path_count,
        evidence_path_count: evidence_path_count,
      )
    end

    def run_git(arguments)
      stdout, _stderr, status = Open3.capture3("git", "-C", @repository_root, *arguments)
      [stdout, status.success?]
    end
  end
end

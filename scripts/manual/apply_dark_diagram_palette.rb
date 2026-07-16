#!/usr/bin/env ruby
# frozen_string_literal: true

require "pathname"

module DarkDiagramPalette
  REPLACEMENTS = {
    "#f7fafc" => "#020617",
    "#ffffff" => "#172033",
    "#f8fafc" => "#182331",
    "#eef2ff" => "#182331",
    "#f0fdf4" => "#182331",
    "#fff7ed" => "#182331",
    "#fdf2f8" => "#182331",
    "#dbeafe" => "#223047",
    "#cffafe" => "#223047",
    "#ede9fe" => "#223047",
    "#e2e8f0" => "#223047",
    "#fef3c7" => "#223047",
    "#dcfce7" => "#223047",
    "#ffe4e6" => "#223047",
    "#fee2e2" => "#223047",
    "#f5f3ff" => "#223047",
    "#fed7aa" => "#223047",
    "#bfdbfe" => "#223047",
    "#eff6ff" => "#223047",
    "#ecfdf5" => "#223047",
    "#fff1f2" => "#223047",
    "#fffbeb" => "#223047",
    "#ecfeff" => "#223047",
    "#e0f2fe" => "#223047",
    "#183247" => "#f8fafc",
    "#587086" => "#d5dfeb",
    "#b6c7dc" => "#d5dfeb",
    "#4b5563" => "#d5dfeb",
    "#cbd5e1" => "#536377",
    "#bae6fd" => "#536377",
    "#bbf7d0" => "#536377",
    "#ddd6fe" => "#536377",
    "#fde68a" => "#536377",
    "#94a3b8" => "#536377",
    "#64748b" => "#536377",
    "#475569" => "#536377",
    "#59677a" => "#d5dfeb",
    "#2563eb" => "#5b8def",
    "#3b82f6" => "#5b8def",
    "#059669" => "#4fb8a8",
    "#10b981" => "#4fb8a8",
    "#d97706" => "#d0a24c",
    "#7c3aed" => "#9b87d8",
    "#8b5cf6" => "#9b87d8",
    "#e11d48" => "#d66f7e",
    "#ec4899" => "#d66f7e",
    "#dc2626" => "#d66f7e",
    "#ef4444" => "#d66f7e",
    "#0891b2" => "#55c7e8",
    "#0284c7" => "#55c7e8",
    "#06b6d4" => "#55c7e8",
    "#6366f1" => "#9b87d8",
    "#818cf8" => "#9b87d8",
    "#22c55e" => "#4fb8a8",
    "#86efac" => "#4fb8a8",
    "#f97316" => "#d0a24c",
    "#f59e0b" => "#d0a24c",
    "#db2777" => "#d66f7e",
    "#334155" => "#000814",
  }.freeze

  module_function

  def transform(content)
    raise ArgumentError, "expected an SVG document" unless content.include?("<svg") && content.include?("</svg>")

    transformed = content.dup
    REPLACEMENTS.each { |from, to| transformed.gsub!(from, to) }
    transformed.gsub!("fill:#172033", "fill:#f8fafc")
    transformed.gsub!("fill:#536377", "fill:#d5dfeb")
    transformed.gsub!('markerWidth="12"', 'markerWidth="14"')
    transformed.gsub!('markerHeight="12"', 'markerHeight="14"')
    transformed.gsub!('stroke-width="2.1"', 'stroke-width="4"')
    transformed.gsub!('stroke-width="2.4"', 'stroke-width="4"')
    transformed.gsub!('stroke-width="2.2"', 'stroke-width="4"')
    transformed.gsub!('stroke-width="2.0"', 'stroke-width="4"')
    transformed.gsub!('stroke-width="1.8"', 'stroke-width="4"')
    transformed.gsub!('stroke-width="1.2"', 'stroke-width="2"')
    transformed.each_line.map do |line|
      line = line.sub("<rect ", '<rect class="card" ') if line.include?('filter="url(#softShadow)"') && !line.include?('class="card"')
      line = line.sub("<path ", '<path class="connector" ') if line.include?("marker-end=") && !line.include?('class="connector"')
      line = line.gsub('stroke-width="1.6"', 'stroke-width="4"') if line.include?('class="connector"')
      line = line.gsub('stroke-width="1.7"', 'stroke-width="4"') if line.include?('class="connector"')
      if line.include?('id="arrowInherit"')
        line = line.gsub(/markerWidth="(?:13|14)"/, 'markerWidth="18"')
        line = line.gsub(/markerHeight="(?:13|14)"/, 'markerHeight="16"')
        line = line.gsub(/refX="(?:12|13)"/, 'refX="17"')
        line = line.gsub(/refY="(?:6\.5|7)"/, 'refY="8"')
        line = line.gsub(/d="M (?:12 6\.5 L 1 1 L 1 12|13 7 L 1 1 L 1 13) Z"/, 'd="M 17 8 L 1 1 L 1 15 Z"')
      end
      line
    end.join
  end
end

if $PROGRAM_NAME == __FILE__
  abort "usage: apply_dark_diagram_palette.rb SVG" unless ARGV.size == 1

  path = Pathname.new(ARGV.fetch(0))
  abort "SVG not found: #{path}" unless path.file? && path.extname == ".svg"

  original = path.read
  transformed = DarkDiagramPalette.transform(original)
  abort "no palette changes needed: #{path}" if transformed == original

  path.write(transformed)
  puts "darkened #{path}"
end

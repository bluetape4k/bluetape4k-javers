#!/usr/bin/env ruby
# frozen_string_literal: true

require "minitest/autorun"
require_relative "apply_dark_diagram_palette"

class ApplyDarkDiagramPaletteTest < Minitest::Test
  def test_converts_light_surfaces_text_accents_and_geometry_metadata
    source = <<~SVG
      <svg xmlns="http://www.w3.org/2000/svg">
        <rect fill="#f7fafc"/>
        <rect fill="#ffffff" stroke="#cbd5e1"/>
        <rect class="node" fill="#dbeafe" stroke="#2563eb" filter="url(#softShadow)"/>
        <rect fill="#eff6ff" stroke="#3b82f6"/>
        <rect fill="#ecfdf5" stroke="#10b981"/>
        <rect fill="#fce7f3" stroke="#db2777"/>
        <path stroke="#fbcfe8"/>
        <path stroke="#fecaca"/>
        <text fill="#183247">Core</text>
        <text fill="#59677a">Supporting detail</text>
        <style>.label{fill:#172033}.section{fill:#4b5563}.member{fill:#475569}</style>
        <path class="line" d="M 0 0 H 10" stroke="#2563eb" stroke-width="2.1" marker-end="url(#arrowBlue)"/>
        <path d="M 0 0 H 10" stroke="#f59e0b" stroke-width="2.2"/>
        <path d="M 0 0 H 10" stroke="#818cf8" stroke-width="2.0"/>
        <path d="M 0 0 H 10" stroke="#86efac" stroke-width="1.8"/>
        <path d="M 0 0 H 10" stroke="#475569" stroke-width="1.6" marker-end="url(#arrowInherit)"/>
        <marker markerWidth="12" markerHeight="12"/>
        <marker id="arrowInherit" markerWidth="13" markerHeight="13" refX="12" refY="6.5"><path d="M 12 6.5 L 1 1 L 1 12 Z"/></marker>
      </svg>
    SVG

    transformed = DarkDiagramPalette.transform(source)

    assert_includes transformed, 'fill="#020617"'
    assert_includes transformed, 'fill="#172033" stroke="#536377"'
    assert_includes transformed, '<rect class="node card" fill="#223047" stroke="#5b8def"'
    assert_includes transformed, '<text fill="#f8fafc">Core</text>'
    assert_includes transformed, '<text fill="#d5dfeb">Supporting detail</text>'
    assert_includes transformed, '.label{fill:#f8fafc}.section{fill:#d5dfeb}.member{fill:#d5dfeb}'
    assert_includes transformed, 'fill="#223047" stroke="#5b8def"'
    assert_includes transformed, 'fill="#223047" stroke="#4fb8a8"'
    assert_includes transformed, 'fill="#223047" stroke="#d66f7e"'
    assert_equal 3, transformed.scan('stroke="#536377"/>').size
    assert_includes transformed, 'stroke="#d0a24c" stroke-width="4"'
    assert_includes transformed, 'stroke="#9b87d8" stroke-width="4"'
    assert_includes transformed, 'stroke="#4fb8a8" stroke-width="4"'
    assert_includes transformed, 'stroke="#536377" stroke-width="4" marker-end="url(#arrowInherit)"'
    assert_includes transformed, '<path class="line connector"'
    refute_match(/class="[^"]*"\s+class="/, transformed)
    assert_includes transformed, 'stroke-width="4"'
    assert_includes transformed, 'markerWidth="14" markerHeight="14"'
    assert_includes transformed, 'id="arrowInherit" markerWidth="18" markerHeight="16" refX="17" refY="8"'
    assert_includes transformed, 'd="M 17 8 L 1 1 L 1 15 Z"'
  end

  def test_rejects_non_svg_content
    error = assert_raises(ArgumentError) { DarkDiagramPalette.transform("not svg") }

    assert_equal "expected an SVG document", error.message
  end
end

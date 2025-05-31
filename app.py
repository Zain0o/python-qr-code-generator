"""
QR Code Generation Web Application

Author: Zain Alshammari

This Flask application provides a web interface for generating Version 1 and 2 QR codes
using custom logic implemented in separate modules. The application demonstrates
 the integration of multiple programming paradigms and third-party libraries.

Key Features:
- Web-based interface for QR code generation
- Support for QR Version 1 (up to 17 bytes) and Version 2 (up to 32 bytes)
- Error Correction Level L implementation using Reed-Solomon codes
- Automatic version selection based on input length
- Optimal mask pattern selection for improved readability
- Real-time QR code rendering as HTML table

Pipeline Overview:
1. Byte-mode encoding and version determination
2. Reed-Solomon error correction (Level L) using the 'reedsolo' library
3. Bitstream structuring including remainder bits
4. Module placement into the QR matrix (finder, separator, timing, alignment patterns)
5. Optimal mask pattern application (evaluating all 8 masks based on penalty scores)
6. Format information string generation and placement
7. Final rendering as scannable QR code

The application follows QR code ISO/IEC 18004:2015 specifications.
"""

from flask import Flask, render_template, request
from data_encoding import encode_byte_mode
from error_correction import generate_error_correction
from matrix_layout import (
    generate_qr_module,
    get_format_string,
    place_format_information,
    get_size_from_version,
)
from matrix_masking import find_best_pattern, create_function_pattern_matrix

app = Flask(__name__)

@app.route('/')
def home():
    return render_template('index.html')

def assemble_qr_matrix(text: str, explain=False):
    # Get data codewords and version
    data_cw, version = encode_byte_mode(text)

    # Convert binary strings to integers for error correction
    data_cw_int = [int(c, 2) for c in data_cw]

    # Generate error correction codewords
    ecc_cw = generate_error_correction(data_cw_int, version)

    # Combine data and ECC codewords
    all_codewords = data_cw + ecc_cw

    # Convert to final bitstream
    final_bits = ''.join(all_codewords)

    # Add remainder bits for Version 2
    if version == 2:
        final_bits += '0' * 7

    # Generate QR matrix with all patterns
    matrix = generate_qr_module(final_bits, version)

    # Create function pattern map
    mask_map = create_function_pattern_matrix(version)

    # Find best mask pattern
    masked_matrix, best_mask = find_best_pattern(matrix, mask_map)

    # Generate format string
    fmt = get_format_string('01', format(best_mask, '03b'))

    # Convert matrix to integers only
    int_matrix = []
    for row in masked_matrix:
        int_row = []
        for cell in row:
            if isinstance(cell, int):
                int_row.append(cell)
            elif cell == 'R':
                int_row.append(0)  # Reserved cells default to 0
            else:
                int_row.append(0)
        int_matrix.append(int_row)

    # Place format information
    final_matrix = place_format_information(int_matrix, fmt, get_size_from_version(version))

    if explain:
        # Generate steps for visualization
        steps = {
            "step1": generate_qr_module(''.join(data_cw), version),
            "step2": generate_qr_module(''.join(data_cw + ecc_cw), version),
            "step3": matrix,
            "step4": masked_matrix,
            "final": final_matrix
        }
        return steps
    # In assemble_qr_matrix, before returning
    print(f"Version: {version}, Matrix size: {len(final_matrix)}x{len(final_matrix[0])}")
    print(f"Data length: {len(text)} chars, {len(text.encode('iso-8859-1'))} bytes")

    return {"final": final_matrix}


def matrix_to_html(matrix, fg="#000000", bg="#ffffff", shape="square", size="medium", frame="none", filter_mode="none"):
    size_map = {"small": 8, "medium": 12, "large": 16}
    px = size_map.get(size, 12)
    shape_style = {
        "square": f"width:{px}px; height:{px}px; background:{{}};",
        "circle": f"width:{px}px; height:{px}px; background:{{}}; border-radius: 50%;"
    }
    style = shape_style.get(shape, shape_style["square"])
    border = "5px solid white" if frame == "thin" else "15px solid white" if frame == "thick" else "none"
    filter_css = "filter: grayscale(100%);" if filter_mode == "grayscale" else "filter: contrast(200%);" if filter_mode == "high-contrast" else ""

    html = f"<table style='border-collapse: collapse; margin: 0 auto; border:{border}; {filter_css}'>\n"
    for row in matrix:
        html += "<tr>"
        for cell in row:
            color = fg if cell else bg
            html += f"<td style='{style.format(color)}'></td>"
        html += "</tr>\n"
    html += "</table>"
    return html


@app.route("/", methods=["GET", "POST"])
def index_page():
    qr_html = None
    error = None
    step_images = []
    text_val = request.form.get('text', '').strip() if request.method == 'POST' else ''
    fg_color = request.form.get("fg_color", "#000000")
    bg_color = request.form.get("bg_color", "#ffffff")
    shape = request.form.get("shape", "square")
    size = request.form.get("size", "medium")
    frame = request.form.get("frame", "none")
    filter_mode = request.form.get("filter", "none")
    explain_steps = request.form.get("explain_steps") == "on"

    if request.method == "POST":
        if not text_val or len(text_val.strip()) == 0:
            error = "Input text cannot be empty."
        elif len(text_val.encode('iso-8859-1', errors='ignore')) > 32:
            error = "Input text too long (max 32 bytes)"
        else:
            try:
                qr_data = assemble_qr_matrix(text_val, explain=explain_steps)
                qr_html = matrix_to_html(qr_data["final"], fg=fg_color, bg=bg_color, shape=shape, size=size,
                                         frame=frame, filter_mode=filter_mode)
                if explain_steps:
                    step_images = [
                        {"title": "Step 1: Byte Encoding", "image": matrix_to_html(qr_data["step1"])},
                        {"title": "Step 2: Error Correction", "image": matrix_to_html(qr_data["step2"])},
                        {"title": "Step 3: Pre-Masking", "image": matrix_to_html(qr_data["step3"])},
                        {"title": "Step 4: Final Masked", "image": matrix_to_html(qr_data["step4"])},
                    ]
            except Exception as e:
                error = f"Error: {str(e)}"

    return render_template("index.html", qr_html=qr_html, text=text_val, error=error,
                           fg_color=fg_color, bg_color=bg_color, shape=shape, size=size,
                           frame=frame, filter=filter_mode, explain_steps=explain_steps,
                           step_images=step_images)


if __name__ == "__main__":
    app.run(debug=True, port=5001)

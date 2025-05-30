"""
QR Code Matrix Layout Module

Author: Zain Alshammari

This module implements QR Code module placement for Version 1 and 2 QR codes.
It handles the creation and population of QR code matrices with all required
patterns and data placement according to ISO/IEC 18004:2015 specifications.

Key Features:
- Finder patterns, separators, and timing patterns placement
- Dark module and reserved areas for format/version information
- Alignment patterns for Version 2 and above
- Zigzag placement algorithm for data and ECC bits
- Support for both Version 1 (21x21) and Version 2 (25x25) matrices

"""

from typing import List, Union

# Type aliases for better code readability
QRMatrix = List[List[Union[int, str, None]]]  # Matrix that can hold 0, 1, 'R' (reserved), or None
FinalQRMatrix = List[List[int]]  # Matrix with only 0s and 1s

# Constants based on Thonky's Error Correction Table for Level L
# Total codewords (data + ECC) for Error Correction Level L
V1_L_TOTAL_CODEWORDS = 26  # Version 1: 19 data + 7 ECC codewords
V2_L_TOTAL_CODEWORDS = 44  # Version 2: 34 data + 10 ECC codewords

# Effective bitstream length (data+ECC+remainder bits) for Level L
V1_EFFECTIVE_BITSTREAM_LENGTH = V1_L_TOTAL_CODEWORDS * 8  # 208 bits (no remainder)
V2_EFFECTIVE_BITSTREAM_LENGTH = V2_L_TOTAL_CODEWORDS * 8 + 7  # 359 bits (7 remainder bits)

# Alignment Pattern Centre Coordinates for each QR version (from ISO/IEC 18004:2015 Annex E)
# Version 1 has no alignment patterns, Version 2 onwards have specific coordinates
ALIGNMENT_PATTERN_COORDS_LOOKUP = {
    1: [],  # No alignment patterns
    2: [6, 18],  # One alignment pattern at (18,18)
    3: [6, 22], 4: [6, 26], 5: [6, 30], 6: [6, 34],
    7: [6, 22, 38], 8: [6, 24, 42], 9: [6, 26, 46], 10: [6, 28, 50],
    11: [6, 30, 54], 12: [6, 32, 58], 13: [6, 34, 62], 14: [6, 26, 46, 66],
    15: [6, 26, 48, 70], 16: [6, 26, 50, 74], 17: [6, 30, 54, 78],
    18: [6, 30, 56, 82], 19: [6, 30, 58, 86], 20: [6, 34, 62, 90],
    21: [6, 28, 50, 72, 94], 22: [6, 26, 50, 74, 98], 23: [6, 30, 54, 78, 102],
    24: [6, 28, 54, 80, 106], 25: [6, 32, 58, 84, 110], 26: [6, 30, 58, 86, 114],
    27: [6, 34, 62, 90, 118], 28: [6, 26, 50, 74, 98, 122], 29: [6, 30, 54, 78, 102, 126],
    30: [6, 26, 52, 78, 104, 130], 31: [6, 30, 56, 82, 108, 134],
    32: [6, 34, 60, 86, 112, 138], 33: [6, 30, 58, 86, 114, 142],
    34: [6, 34, 62, 90, 118, 146], 35: [6, 30, 54, 78, 102, 126, 150],
    36: [6, 24, 50, 76, 102, 128, 154], 37: [6, 28, 54, 80, 106, 132, 158],
    38: [6, 32, 58, 84, 110, 136, 162], 39: [6, 26, 54, 82, 110, 138, 166],
    40: [6, 30, 58, 86, 114, 142, 170],
}

# Primary format information bit coordinates (15 bits total)
# These coordinates define where format information is placed around the top-left finder pattern
# Order matches bit order of format string (bits[0] to bits[14])
FORMAT_INFO_COORDINATES_PRIMARY = [
    (8, 0), (8, 1), (8, 2), (8, 3), (8, 4), (8, 5), (8, 7),  # Bits 0-6 (horizontal)
    (8, 8),  # Bit 7 (The "dark module" cell for format string)
    (7, 8), (5, 8), (4, 8), (3, 8), (2, 8), (1, 8), (0, 8)  # Bits 8-14 (vertical)
]


def _bch_poly_divide(message_poly_str: str, generator_poly_str: str, ecc_len: int) -> str:
    """
    Perform BCH polynomial division for error correction.

    This function implements the polynomial division algorithm used in BCH
    (Bose-Chaudhuri-Hocquenghem) error correction codes. It's used to calculate
    the error correction bits for QR code format information.

    Args:
        message_poly_str: Binary string representing the message polynomial
        generator_poly_str: Binary string representing the generator polynomial
        ecc_len: Length of the error correction code to generate

    Returns:
        str: Binary string of the remainder (error correction bits)
    """
    num_message_bits = len(message_poly_str)
    num_gen_bits = len(generator_poly_str)

    # Convert binary strings to lists of integers for XOR operations
    data_as_ints = [int(bit) for bit in message_poly_str]
    gen_as_ints = [int(bit) for bit in generator_poly_str]

    # Perform polynomial division using XOR
    for i in range(num_message_bits - ecc_len):
        if data_as_ints[i] == 1:
            for j in range(num_gen_bits):
                data_as_ints[i + j] ^= gen_as_ints[j]

    # Extract the remainder (last ecc_len bits)
    remainder_bits = "".join(map(str, data_as_ints[-ecc_len:]))
    return remainder_bits


def get_format_string(ecc_level_indicator_str: str, mask_pattern_indicator_str: str) -> str:
    """
    Generate the 15-bit format string for QR code format information.

    The format string contains error correction level and mask pattern information,
    protected by BCH error correction and XOR masking to ensure it's never all zeros.

    Args:
        ecc_level_indicator_str: 2-bit string for ECC level ('01' for Level L)
        mask_pattern_indicator_str: 3-bit string for mask pattern (e.g., '000' for pattern 0)

    Returns:
        str: 15-bit format string ready to be placed in the QR code
    """
    # Combine ECC level and mask pattern into 5-bit data
    data_5bit_str = ecc_level_indicator_str + mask_pattern_indicator_str

    # Prepare for BCH encoding by appending 10 zeros
    message_poly_str_for_bch = data_5bit_str + '0' * 10

    # Generator polynomial for format information (x^10 + x^8 + x^5 + x^4 + x^2 + x + 1)
    generator_poly_str_bch = "10100110111"

    # Calculate 10-bit error correction code
    ecc_10bit_str = _bch_poly_divide(message_poly_str_for_bch, generator_poly_str_bch, 10)

    # Combine data and ECC
    format_info_no_mask_str = data_5bit_str + ecc_10bit_str

    # Apply XOR mask to ensure format string is never all zeros
    final_xor_mask_val = 0b101010000010010
    final_format_val = int(format_info_no_mask_str, 2) ^ final_xor_mask_val

    return format(final_format_val, '015b')


def create_matrix(size: int) -> QRMatrix:
    """
    Create an empty QR code matrix of the specified size.

    Args:
        size: The dimension of the square matrix (e.g., 21 for Version 1)

    Returns:
        QRMatrix: A size x size matrix filled with None values
    """
    return [[None for _ in range(size)] for _ in range(size)]


def place_finder_pattern(matrix: QRMatrix, r_start: int, c_start: int) -> None:
    """
    Place a single 7x7 finder pattern at the specified position.

    Finder patterns are the large square patterns at three corners of the QR code
    that help scanners locate and orient the code.

    Args:
        matrix: The QR matrix to modify
        r_start: Starting row position for the finder pattern
        c_start: Starting column position for the finder pattern
    """
    # Standard 7x7 finder pattern: outer dark ring, inner light ring, dark centre
    pattern = [
        [1, 1, 1, 1, 1, 1, 1],
        [1, 0, 0, 0, 0, 0, 1],
        [1, 0, 1, 1, 1, 0, 1],
        [1, 0, 1, 1, 1, 0, 1],
        [1, 0, 1, 1, 1, 0, 1],
        [1, 0, 0, 0, 0, 0, 1],
        [1, 1, 1, 1, 1, 1, 1]
    ]

    # Copy pattern to matrix
    for r_offset in range(7):
        for c_offset in range(7):
            matrix[r_start + r_offset][c_start + c_offset] = pattern[r_offset][c_offset]


def place_finder_patterns(matrix: QRMatrix, size: int) -> None:
    """
    Place all three finder patterns in their standard positions.

    Args:
        matrix: The QR matrix to modify
        size: The dimension of the QR code
    """
    # Top-left corner
    place_finder_pattern(matrix, 0, 0)
    # Top-right corner
    place_finder_pattern(matrix, 0, size - 7)
    # Bottom-left corner
    place_finder_pattern(matrix, size - 7, 0)


def add_separators(matrix: QRMatrix, size: int) -> None:
    """
    Add white separator borders around all three finder patterns.

    Separators are single-module-wide white borders that surround the finder
    patterns to ensure they're clearly distinguishable from the data area.

    Args:
        matrix: The QR matrix to modify
        size: The dimension of the QR code
    """
    # Top-left finder pattern separators
    for i in range(8):
        matrix[i][7] = 0  # Vertical separator
        matrix[7][i] = 0  # Horizontal separator

    # Top-right finder pattern separators
    for i in range(8):
        matrix[i][size - 8] = 0  # Vertical separator
        matrix[7][size - 1 - i] = 0  # Horizontal separator

    # Bottom-left finder pattern separators
    for i in range(8):
        matrix[size - 8][i] = 0  # Horizontal separator
        matrix[size - 1 - i][7] = 0  # Vertical separator


def place_alignment_pattern(matrix: QRMatrix, center_r: int, center_c: int) -> None:
    """
    Place a single 5x5 alignment pattern at the specified centre position.

    Alignment patterns help scanners correct for image distortion and are
    required for Version 2 and above.

    Args:
        matrix: The QR matrix to modify
        center_r: Centre row position for the alignment pattern
        center_c: Centre column position for the alignment pattern
    """
    # Standard 5x5 alignment pattern: dark outer ring, light inner ring, dark centre
    pattern = [
        [1, 1, 1, 1, 1],
        [1, 0, 0, 0, 1],
        [1, 0, 1, 0, 1],
        [1, 0, 0, 0, 1],
        [1, 1, 1, 1, 1]
    ]

    # Place pattern centred at the specified position
    for r_offset in range(-2, 3):
        for c_offset in range(-2, 3):
            r, c = center_r + r_offset, center_c + c_offset
            # Only place if within bounds and cell is empty
            if 0 <= r < len(matrix) and 0 <= c < len(matrix[0]):
                if matrix[r][c] is None:
                    matrix[r][c] = pattern[r_offset + 2][c_offset + 2]


def place_alignment_patterns(matrix: QRMatrix, version: int) -> None:
    """
    Place all alignment patterns for the specified QR version.

    This function determines all possible alignment pattern positions based on
    the version and places them, avoiding overlaps with finder patterns.

    Args:
        matrix: The QR matrix to modify
        version: The QR code version (1-40)
    """
    if version < 2:
        return  # Version 1 has no alignment patterns

    coords_list = ALIGNMENT_PATTERN_COORDS_LOOKUP.get(version, [])
    if not coords_list:
        return

    size = len(matrix)

    # Generate all possible centre positions from the coordinate list
    all_possible_centers = []
    for r_val in coords_list:
        for c_val in coords_list:
            all_possible_centers.append((r_val, c_val))

    # Place each alignment pattern if it doesn't overlap with finder patterns
    for r_center, c_center in all_possible_centers:
        overlaps_finder = False

        # Define finder pattern zones (including separators)
        finder_zones = [
            (0, 0, 7, 7),  # Top-left
            (0, size - 8, 7, size - 1),  # Top-right
            (size - 8, 0, size - 1, 7)  # Bottom-left
        ]

        # Calculate alignment pattern bounds
        r_align_start, c_align_start = r_center - 2, c_center - 2
        r_align_end, c_align_end = r_center + 2, c_center + 2

        # Check for overlap with any finder zone
        for fr_start, fc_start, fr_end, fc_end in finder_zones:
            if not (r_align_end < fr_start or r_align_start > fr_end or
                    c_align_end < fc_start or c_align_start > fc_end):
                overlaps_finder = True
                break

        if not overlaps_finder:
            place_alignment_pattern(matrix, r_center, c_center)


def place_timing_patterns(matrix: QRMatrix, size: int) -> None:
    """
    Place horizontal and vertical timing patterns.

    Timing patterns are alternating dark/light modules that help scanners
    determine module coordinates within the QR code.

    Args:
        matrix: The QR matrix to modify
        size: The dimension of the QR code
    """
    # Place timing patterns between finder patterns
    for i in range(8, size - 8):
        # Alternating pattern: dark for even positions, light for odd
        val = 1 if i % 2 == 0 else 0

        # Horizontal timing pattern (row 6)
        if matrix[6][i] is None:
            matrix[6][i] = val

        # Vertical timing pattern (column 6)
        if matrix[i][6] is None:
            matrix[i][6] = val


def reserve_format_info_areas(matrix: QRMatrix, size: int) -> None:
    """
    Reserve areas for format information by marking them with 'R'.

    Format information areas store error correction level and mask pattern data.
    These areas are reserved during initial matrix creation and filled later.

    Args:
        matrix: The QR matrix to modify
        size: The dimension of the QR code
    """
    # Reserve primary format info area (around top-left finder)
    for r_coord, c_coord in FORMAT_INFO_COORDINATES_PRIMARY:
        if 0 <= r_coord < size and 0 <= c_coord < size and matrix[r_coord][c_coord] is None:
            matrix[r_coord][c_coord] = 'R'

    # Reserve top-right copy area
    for j_val in range(8):
        if matrix[8][size - 1 - j_val] is None:
            matrix[8][size - 1 - j_val] = 'R'

    # Reserve bottom-left copy area
    for j_val in range(8):
        if matrix[size - 1 - j_val][8] is None:
            matrix[size - 1 - j_val][8] = 'R'


def place_dark_module(matrix: QRMatrix, size: int) -> None:
    """
    Ensure the dark module position is properly reserved.

    The dark module is a specific module that must always be dark (black) in the
    final QR code. It's located at position (4*version+9, 8) in 0-indexed coordinates.

    Args:
        matrix: The QR matrix to modify
        size: The dimension of the QR code
    """
    # Calculate dark module position
    dark_module_row = size - 8  # Equals 4*version + 9 in 0-indexed
    dark_module_col = 8

    # Ensure it's marked as reserved if not already set
    if 0 <= dark_module_row < size and 0 <= dark_module_col < size:
        if matrix[dark_module_row][dark_module_col] is None:
            matrix[dark_module_row][dark_module_col] = 'R'


def place_data_bits(matrix: QRMatrix, size: int, data_bits_str: str) -> None:
    """
    Place data and error correction bits using the QR code zigzag pattern.

    This function implements the specific zigzag placement algorithm that QR codes
    use to distribute data throughout the matrix, avoiding reserved areas.

    Args:
        matrix: The QR matrix to modify
        size: The dimension of the QR code
        data_bits_str: Binary string containing all data and ECC bits to place
    """
    bit_idx = 0
    row, col = size - 1, size - 1  # Start at bottom-right
    direction = -1  # -1 for upward, 1 for downward

    # Process columns from right to left, two at a time
    while col >= 0:
        # Skip the vertical timing column
        if col == 6:
            col -= 1
            continue

        current_path_col = col

        # Process all rows in current column pair
        for _ in range(size):
            # Process both columns in the pair (right then left)
            for c_offset in range(2):
                current_c = current_path_col - c_offset

                # Only place bit if cell is empty
                if 0 <= current_c < size and matrix[row][current_c] is None:
                    # Place data bit or 0 if we've run out of data
                    if bit_idx < len(data_bits_str):
                        matrix[row][current_c] = int(data_bits_str[bit_idx])
                        bit_idx += 1
                    else:
                        matrix[row][current_c] = 0

            # Move to next row
            row += direction

            # If we've hit the edge, reverse direction and move to next column pair
            if not (0 <= row < size):
                direction *= -1
                row += direction
                col -= 2
                break

        # Exit if we've processed all columns
        if col < 0:
            break

    # Fill any remaining empty cells with 0 (shouldn't happen with correct data)
    for r_idx in range(size):
        for c_idx in range(size):
            if matrix[r_idx][c_idx] is None:
                matrix[r_idx][c_idx] = 0


def generate_qr_module(final_bitstream: str, version: int) -> QRMatrix:
    """
    Generate a complete QR code matrix with all patterns and data placed.

    This is the main function that orchestrates the creation of a QR code matrix
    by calling all the necessary placement functions in the correct order.

    Args:
        final_bitstream: Complete binary string containing all data, ECC, and remainder bits
        version: The QR code version (1 or 2 supported)

    Returns:
        QRMatrix: Complete matrix with all patterns and data placed
    """
    size = get_size_from_version(version)
    matrix = create_matrix(size)

    # Place all structural patterns in order
    place_finder_patterns(matrix, size)
    add_separators(matrix, size)

    if version >= 2:
        place_alignment_patterns(matrix, version)

    place_timing_patterns(matrix, size)
    reserve_format_info_areas(matrix, size)  # Mark format areas with 'R'
    place_dark_module(matrix, size)  # Ensure dark module is reserved

    # Place all data bits using zigzag pattern
    place_data_bits(matrix, size, final_bitstream)

    return matrix


def place_format_information(matrix: FinalQRMatrix, fmt: str, size: int) -> FinalQRMatrix:
    """
    Place format information bits in all three required locations.

    Format information is placed in three copies around the QR code to ensure
    it can be read even if parts of the code are damaged.

    Args:
        matrix: The QR matrix (must contain only integers 0 and 1)
        fmt: 15-bit format string to place
        size: The dimension of the QR code

    Returns:
        FinalQRMatrix: The matrix with format information placed
    """
    bits = [int(b) for b in fmt]

    # Primary 15 bits around top-left finder pattern
    for i, (r, c) in enumerate(FORMAT_INFO_COORDINATES_PRIMARY):
        matrix[r][c] = bits[i]

    # Secondary copy around top-right finder pattern
    # Bits 0-7 placed horizontally from (8, size-1) to (8, size-8)
    for i in range(8):
        matrix[8][size - 1 - i] = bits[i]

    # Bits 8-14 placed vertically from (size-7, 8) to (0, 8), skipping timing row
    idx = 8
    for r_coord in range(8):
        if r_coord == 6:  # Skip timing pattern row
            continue
        matrix[r_coord][8] = bits[idx]
        idx += 1

    # Secondary copy around bottom-left finder pattern
    # Bits 0-7 placed vertically from (size-1, 8) to (size-8, 8)
    idx = 0
    for i in range(8):
        matrix[size - 1 - i][8] = bits[idx]
        idx += 1

    # Bits 8-14 placed horizontally from (8, 0) to (8, 7), skipping timing column
    idx = 8
    version = get_version_from_size(size)

    for c_coord in range(8):
        if c_coord == 6:  # Skip timing pattern column
            continue
        matrix[8][c_coord] = bits[idx]
        idx += 1

        dark_row = 4 * version + 9
        dark_col = 8
        matrix[dark_row][dark_col] = 1  # Force dark module

        return matrix


def get_size_from_version(version: int) -> int:
    """
    Calculate QR code matrix size from version number.

    Args:
        version: QR code version (1-40)

    Returns:
        int: Matrix dimension (e.g., 21 for Version 1, 25 for Version 2)

    Raises:
        ValueError: If version is not between 1 and 40
    """
    if not 1 <= version <= 40:
        raise ValueError(f"Version must be between 1 and 40, got {version}")

    # Formula: size = (version - 1) * 4 + 21
    return (version - 1) * 4 + 21


def get_version_from_size(size: int) -> int:
    """
    Determine QR code version from matrix size.

    Args:
        size: Matrix dimension

    Returns:
        int: QR code version (1-40)

    Raises:
        ValueError: If size doesn't correspond to a valid QR version
    """
    if (size - 21) % 4 != 0 or not (21 <= size <= 177):
        raise ValueError(f"Invalid QR matrix size: {size} for version determination.")

    return (size - 21) // 4 + 1


def get_expected_bitstream_length_for_version(version: int) -> int:
    """
    Get the expected total bitstream length for a specific version at Level L.

    Args:
        version: QR code version (currently supports 1 and 2)

    Returns:
        int: Total number of bits including data, ECC, and remainder

    Raises:
        ValueError: If version is not supported
    """
    if version == 1:
        return V1_EFFECTIVE_BITSTREAM_LENGTH
    if version == 2:
        return V2_EFFECTIVE_BITSTREAM_LENGTH

    raise ValueError(f"Expected bitstream length not defined for V{version}-L in this context.")


def print_matrix(matrix: FinalQRMatrix) -> None:
    """
    Print a QR matrix to console using block characters for visualisation.

    Dark modules (1) are displayed as filled blocks, light modules (0) as spaces.

    Args:
        matrix: The QR matrix to print (must contain only 0s and 1s)
    """
    if not matrix or not matrix[0]:
        print("Empty matrix.")
        return

    # Use double-width characters for better aspect ratio
    for row_list in matrix:
        print("".join(["██" if module == 1 else "  " for module in row_list]))


# Test code for module functionality
if __name__ == "__main__":
    import matrix_masking  # Import for testing mask pattern functionality

    print("--- Testing Version 1 (e.g., 'HELLO WORLD' with a representative bitstream) ---")

    # Example bitstream for Version 1 with Level L error correction
    # This represents a complete encoded message including mode, count, data, and ECC
    test_v1_final_bitstream = "0010000001011011000010110111100011010001011100101101110001001101010000110100001110110000010001111011000001000111101100000100011101100110001001001000001010111110100110111111111100000001111001100010101011010101010001111110111110111100"
    print(f"Test V1 Bitstream length: {len(test_v1_final_bitstream)}")

    if len(test_v1_final_bitstream) == V1_EFFECTIVE_BITSTREAM_LENGTH:
        # Generate initial matrix with patterns and data
        v1_matrix_intermediate = generate_qr_module(test_v1_final_bitstream, version=1)

        print(
            "\nVersion 1 Matrix (After generate_qr_module, 'R' for Format - data possibly not final visually until masking):")
        for r_idx, row_val in enumerate(v1_matrix_intermediate):
            print(f"Row {r_idx:02d}: {''.join(str(c) if c is not None else '_' for c in row_val)}")

        # Apply masking to find best pattern
        func_map_v1 = matrix_masking.create_function_pattern_matrix(1)
        masked_v1_with_R, best_mask_v1 = matrix_masking.find_best_pattern(v1_matrix_intermediate, func_map_v1)

        # Generate format string for the best mask
        format_str_v1 = get_format_string("01", format(best_mask_v1, '03b'))
        print(f"V1 Format string for L,{best_mask_v1}: {format_str_v1}")

        # Convert matrix to integer-only format for final format placement
        temp_v1_int_matrix = [[val if isinstance(val, int) else 0 for val in r_list] for r_list in masked_v1_with_R]

        # Place format information and display final result
        final_v1_qr = place_format_information(temp_v1_int_matrix, format_str_v1, 21)
        print("\nFinal Version 1 Matrix (Masked, with Format Info):")
        print_matrix(final_v1_qr)
    else:
        print(f"V1 test bitstream length incorrect: {len(test_v1_final_bitstream)} vs {V1_EFFECTIVE_BITSTREAM_LENGTH}")

    print("\n--- Testing Version 2 (e.g., 'known' with a representative bitstream) ---")

    # Example bitstream for Version 2 - demonstrating with 'known' encoded
    dummy_v2_payload = "0100000010101101011011011110111011101101110"  # Mode + Count + "known" data
    remaining_for_v2 = V2_EFFECTIVE_BITSTREAM_LENGTH - len(dummy_v2_payload)
    test_v2_final_bitstream = dummy_v2_payload + "0" * remaining_for_v2
    print(f"Test V2 Final Bitstream Length: {len(test_v2_final_bitstream)}")

    if len(test_v2_final_bitstream) == V2_EFFECTIVE_BITSTREAM_LENGTH:
        # Generate Version 2 matrix
        v2_matrix_intermediate = generate_qr_module(test_v2_final_bitstream, version=2)

        # Apply masking to find best pattern
        func_map_v2 = matrix_masking.create_function_pattern_matrix(2)
        masked_v2_with_R, best_mask_v2 = matrix_masking.find_best_pattern(v2_matrix_intermediate, func_map_v2)

        # Generate format string and create final matrix
        format_str_v2 = get_format_string("01", format(best_mask_v2, '03b'))
        print(f"V2 Format string for L,{best_mask_v2}: {format_str_v2}")

        temp_v2_int_matrix = [[val if isinstance(val, int) else 0 for val in r_list] for r_list in masked_v2_with_R]

        final_v2_qr = place_format_information(temp_v2_int_matrix, format_str_v2, 25)
        print("\nFinal Version 2 Matrix (Masked, with Format Info):")
        print_matrix(final_v2_qr)
    else:
        print(f"V2 test bitstream length incorrect: {len(test_v2_final_bitstream)} vs {V2_EFFECTIVE_BITSTREAM_LENGTH}")

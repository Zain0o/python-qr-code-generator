"""
QR Code Matrix Masking Module

Author: Zain Alshammari


This module implements QR code mask pattern application and evaluation according to
ISO/IEC 18004:2015 specifications. It provides functionality to apply all eight
standard mask patterns and determine the optimal pattern based on penalty scoring.

Key Features:
- Implementation of all 8 QR code mask patterns (0-7)
- Function pattern matrix generation to identify maskable regions
- Penalty score calculation using all four QR code penalty rules
- Automatic selection of optimal mask pattern for minimal penalty

The module works in conjunction with matrix_layout to ensure proper QR code
generation with optimal readability.
"""

import math
from typing import List, Tuple, Union
import matrix_layout  # For get_size_from_version, ALIGNMENT_PATTERN_COORDS_LOOKUP, FORMAT_INFO_COORDINATES_PRIMARY

# Type aliases for clarity
QRMatrixWithPlaceholders = List[List[Union[int, str, None]]]  # Can contain 'R' for reserved
QRMatrix = List[List[int]]  # Pure integer matrix for penalty calculations


# Mask pattern condition functions according to ISO/IEC 18004:2015
def _should_mask_pattern_0(r: int, c: int) -> bool:
    """Pattern 0: (row + column) mod 2 == 0"""
    return (r + c) % 2 == 0


def _should_mask_pattern_1(r: int, c: int) -> bool:
    """Pattern 1: row mod 2 == 0"""
    return r % 2 == 0


def _should_mask_pattern_2(r: int, c: int) -> bool:
    """Pattern 2: column mod 3 == 0"""
    return c % 3 == 0


def _should_mask_pattern_3(r: int, c: int) -> bool:
    """Pattern 3: (row + column) mod 3 == 0"""
    return (r + c) % 3 == 0


def _should_mask_pattern_4(r: int, c: int) -> bool:
    """Pattern 4: (floor(row/2) + floor(column/3)) mod 2 == 0"""
    return (math.floor(r / 2) + math.floor(c / 3)) % 2 == 0


def _should_mask_pattern_5(r: int, c: int) -> bool:
    """Pattern 5: ((row * column) mod 2) + ((row * column) mod 3) == 0"""
    return ((r * c) % 2) + ((r * c) % 3) == 0


def _should_mask_pattern_6(r: int, c: int) -> bool:
    """Pattern 6: (((row * column) mod 2) + ((row * column) mod 3)) mod 2 == 0"""
    return (((r * c) % 2) + ((r * c) % 3)) % 2 == 0


def _should_mask_pattern_7(r: int, c: int) -> bool:
    """Pattern 7: (((row + column) mod 2) + ((row * column) mod 3)) mod 2 == 0"""
    return (((r + c) % 2) + ((r * c) % 3)) % 2 == 0


# Dictionary mapping pattern IDs to their condition functions
MASK_CONDITION_FUNCTIONS = {
    0: _should_mask_pattern_0,
    1: _should_mask_pattern_1,
    2: _should_mask_pattern_2,
    3: _should_mask_pattern_3,
    4: _should_mask_pattern_4,
    5: _should_mask_pattern_5,
    6: _should_mask_pattern_6,
    7: _should_mask_pattern_7
}


def apply_specific_mask_pattern(
        pattern_id: int,
        matrix: QRMatrixWithPlaceholders,
        function_map: List[List[bool]]
) -> QRMatrixWithPlaceholders:
    """
    Apply a specific mask pattern to the QR matrix.

    Masking inverts (XORs) module values in data regions only, leaving function
    patterns (finders, timing, format info, etc.) unchanged. This improves QR
    code readability by breaking up patterns of same-coloured modules.

    Args:
        pattern_id: Mask pattern ID (0-7)
        matrix: QR matrix with data, function patterns, and placeholders
        function_map: Boolean matrix marking function pattern locations (True = don't mask)

    Returns:
        QRMatrixWithPlaceholders: New matrix with mask pattern applied

    Raises:
        ValueError: If pattern_id is invalid or matrix dimensions don't match
    """
    if not 0 <= pattern_id <= 7:
        raise ValueError("Mask ID must be 0-7.")

    # Validate matrix dimensions
    rows = len(matrix)
    cols = len(matrix[0]) if matrix and matrix[0] else 0

    if not matrix or len(function_map) != rows or (0 < cols != len(function_map[0])):
        raise ValueError("Dimension mismatch or empty matrix/function_map.")

    # Create deep copy to avoid modifying original
    out_matrix = [row[:] for row in matrix]
    mask_func = MASK_CONDITION_FUNCTIONS[pattern_id]

    # Apply mask only to data regions (non-functional integer cells)
    for r in range(rows):
        for c in range(cols):
            # Only mask if: not a function pattern AND is an integer value
            if not function_map[r][c] and isinstance(out_matrix[r][c], int):
                if mask_func(r, c):
                    out_matrix[r][c] ^= 1  # XOR with 1 to invert

    return out_matrix


def create_function_pattern_matrix(version: int) -> List[List[bool]]:
    """
    Create a boolean matrix marking all function pattern locations.

    Function patterns include finder patterns, separators, timing patterns,
    format information areas, dark module, and alignment patterns. These areas
    are not affected by masking.

    Args:
        version: QR code version (1-40)

    Returns:
        List[List[bool]]: Matrix where True = function pattern, False = data/ECC area
    """
    size = matrix_layout.get_size_from_version(version)
    func_matrix = [[False for _ in range(size)] for _ in range(size)]

    def mark_area(r_start: int, c_start: int, r_len: int, c_len: int) -> None:
        """Mark a rectangular area as function pattern."""
        for r_val in range(r_start, r_start + r_len):
            for c_val in range(c_start, c_start + c_len):
                if 0 <= r_val < size and 0 <= c_val < size:
                    func_matrix[r_val][c_val] = True

    # Mark finder patterns (7x7) plus separators (1 module wide)
    # This creates 8x8 protected areas in three corners
    mark_area(0, 0, 8, 8)  # Top-left
    mark_area(0, size - 8, 8, 8)  # Top-right
    mark_area(size - 8, 0, 8, 8)  # Bottom-left

    # Mark timing patterns (alternating modules between finders)
    for i in range(8, size - 8):
        func_matrix[6][i] = True  # Horizontal timing
        func_matrix[i][6] = True  # Vertical timing

    # Mark format information areas (15 bits around finders)
    # Primary format area (around top-left finder)
    for r_coord, c_coord in matrix_layout.FORMAT_INFO_COORDINATES_PRIMARY:
        if 0 <= r_coord < size and 0 <= c_coord < size:
            func_matrix[r_coord][c_coord] = True

    # Secondary format info copies (for redundancy)
    # Top-right horizontal copy
    for j_val in range(8):
        func_matrix[8][size - 1 - j_val] = True

    # Bottom-left vertical copy
    for i_val in range(8):
        func_matrix[size - 1 - i_val][8] = True

    # Note: Dark module at (size-8, 8) is already marked as format info area

    # Mark alignment patterns (Version 2 and above)
    if version >= 2:
        coords_ref_list = matrix_layout.ALIGNMENT_PATTERN_COORDS_LOOKUP.get(version, [])

        # Generate all possible alignment pattern centres
        all_possible_centers_for_version = []
        if coords_ref_list:
            for r_coord_val in coords_ref_list:
                for c_coord_val in coords_ref_list:
                    # Skip centres that would overlap with finder pattern centres
                    if (r_coord_val == 6 and c_coord_val == 6) or \
                            (r_coord_val == 6 and c_coord_val == size - 7) or \
                            (r_coord_val == size - 7 and c_coord_val == 6):
                        continue
                    all_possible_centers_for_version.append((r_coord_val, c_coord_val))

        # Place alignment patterns where they don't overlap with finder areas
        for r_center, c_center in all_possible_centers_for_version:
            # Check for overlap with 8x8 finder pattern areas
            overlaps_finder_area = False
            finder_areas = [(0, 0, 8, 8), (0, size - 8, 8, 8), (size - 8, 0, 8, 8)]

            for fr, fc, flr, flc in finder_areas:
                # Alignment pattern spans 5x5 centred at (r_center, c_center)
                if not (r_center + 2 < fr or r_center - 2 > fr + flr - 1 or
                        c_center + 2 < fc or c_center - 2 > fc + flc - 1):
                    overlaps_finder_area = True
                    break

            if overlaps_finder_area:
                continue

            # Mark 5x5 alignment pattern area
            for r_offset in range(-2, 3):
                for c_offset in range(-2, 3):
                    if 0 <= r_center + r_offset < size and 0 <= c_center + c_offset < size:
                        func_matrix[r_center + r_offset][c_center + c_offset] = True

    return func_matrix


def _calculate_penalty_rule1(matrix: QRMatrix) -> int:
    """
    Calculate penalty for Rule 1: Adjacent modules in row/column in same colour.

    Penalty is 3 points plus 1 for each module beyond 5 in a consecutive group.

    Args:
        matrix: QR matrix to evaluate

    Returns:
        int: Total penalty score for Rule 1
    """
    penalty = 0
    size = len(matrix)

    # Check rows for consecutive modules
    for r_idx in range(size):
        count = 1
        current_val = matrix[r_idx][0] if size > 0 else -1

        for c_idx in range(1, size):
            if matrix[r_idx][c_idx] == current_val:
                count += 1
            else:
                # End of consecutive group
                if count >= 5:
                    penalty += (3 + (count - 5))
                current_val = matrix[r_idx][c_idx]
                count = 1

        # Check last group in row
        if count >= 5:
            penalty += (3 + (count - 5))

    # Check columns for consecutive modules
    for c_idx in range(size):
        count = 1
        current_val = matrix[0][c_idx] if size > 0 else -1

        for r_idx in range(1, size):
            if matrix[r_idx][c_idx] == current_val:
                count += 1
            else:
                # End of consecutive group
                if count >= 5:
                    penalty += (3 + (count - 5))
                current_val = matrix[r_idx][c_idx]
                count = 1

        # Check last group in column
        if count >= 5:
            penalty += (3 + (count - 5))

    return penalty


def _calculate_penalty_rule2(matrix: QRMatrix) -> int:
    """
    Calculate penalty for Rule 2: 2x2 blocks of same colour.

    Each 2x2 block of the same colour incurs 3 penalty points.

    Args:
        matrix: QR matrix to evaluate

    Returns:
        int: Total penalty score for Rule 2
    """
    penalty = 0
    size = len(matrix)

    # Check all possible 2x2 blocks
    for r_idx in range(size - 1):
        for c_idx in range(size - 1):
            # Check if all four modules in 2x2 block are the same
            if matrix[r_idx][c_idx] == matrix[r_idx + 1][c_idx] and \
                    matrix[r_idx][c_idx] == matrix[r_idx][c_idx + 1] and \
                    matrix[r_idx][c_idx] == matrix[r_idx + 1][c_idx + 1]:
                penalty += 3

    return penalty


def _calculate_penalty_rule3(matrix: QRMatrix) -> int:
    """
    Calculate penalty for Rule 3: Specific patterns resembling finder patterns.

    Looks for patterns of 1:1:3:1:1 ratio (dark:light:dark:light:dark) with
    4 light modules on either side. Each occurrence incurs 40 penalty points.

    Args:
        matrix: QR matrix to evaluate

    Returns:
        int: Total penalty score for Rule 3
    """
    penalty = 0
    size = len(matrix)

    # Define patterns to search for (as per Thonky specification)
    # Pattern: LLLL D L DDD L D or D L DDD L D LLLL
    patterns_to_check_horizontal = [
        [0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 1],  # 00001011101
        [1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0]  # 10111010000
    ]
    pat_len = 11

    # Check rows for patterns
    for r_idx in range(size):
        for c_idx in range(size - pat_len + 1):
            current_row_slice = matrix[r_idx][c_idx: c_idx + pat_len]
            if current_row_slice == patterns_to_check_horizontal[0] or \
                    current_row_slice == patterns_to_check_horizontal[1]:
                penalty += 40

    # Check columns for patterns
    for c_idx in range(size):
        for r_idx in range(size - pat_len + 1):
            current_col_slice = [matrix[k][c_idx] for k in range(r_idx, r_idx + pat_len)]
            if current_col_slice == patterns_to_check_horizontal[0] or \
                    current_col_slice == patterns_to_check_horizontal[1]:
                penalty += 40

    return penalty


def _calculate_penalty_rule4(matrix: QRMatrix) -> int:
    """
    Calculate penalty for Rule 4: Proportion of dark modules.

    Penalty based on deviation from 50% dark modules. Each 5% deviation
    (or part thereof) from 50% incurs 10 penalty points.

    Args:
        matrix: QR matrix to evaluate

    Returns:
        int: Total penalty score for Rule 4
    """
    size = len(matrix)
    if size == 0:
        return 0

    # Count total modules and dark modules
    total_modules = size * size
    dark_modules = sum(row.count(1) for row in matrix)

    # Calculate percentage of dark modules
    percent_dark = (dark_modules / total_modules) * 100.0

    # Calculate deviation from 50% in 5% steps
    deviation = abs(percent_dark - 50)
    penalty_factor = math.floor(deviation / 5)

    return penalty_factor * 10


def calculate_total_penalty_score(matrix: QRMatrix, mask_id_for_debug: int = -1) -> Tuple[int, List[int]]:
    """
    Calculate the total penalty score for a masked QR matrix.

    Applies all four penalty rules and returns both total and individual scores.
    Used to determine the optimal mask pattern.

    Args:
        matrix: QR matrix to evaluate (must contain only 0s and 1s)
        mask_id_for_debug: Mask pattern ID for debug output

    Returns:
        Tuple[int, List[int]]: Total penalty score and list of individual rule penalties
    """
    # Validate matrix
    if not matrix or (len(matrix) > 0 and not matrix[0]):
        print(
            f"Warning: calculate_total_penalty_score received an empty or malformed matrix for mask {mask_id_for_debug}")
        return 10 ^ 18, [10 ^ 18] * 4

    # Calculate individual penalties for each rule
    penalties = [
        _calculate_penalty_rule1(matrix),
        _calculate_penalty_rule2(matrix),
        _calculate_penalty_rule3(matrix),
        _calculate_penalty_rule4(matrix)
    ]

    total_score = sum(penalties)

    return total_score, penalties


def find_best_pattern(
        base_qr_matrix_with_placeholders: QRMatrixWithPlaceholders,
        function_map: List[List[bool]]
) -> Tuple[QRMatrixWithPlaceholders, int]:
    """
    Evaluate all mask patterns and select the one with lowest penalty score.

    This function applies each of the 8 mask patterns to the QR matrix,
    calculates penalty scores, and returns the optimal masked matrix.
    Reserved ('R') cells are treated as light (0) modules for penalty calculation.

    Args:
        base_qr_matrix_with_placeholders: QR matrix with data and 'R' placeholders
        function_map: Boolean matrix marking function pattern locations

    Returns:
        Tuple containing:
        - QRMatrixWithPlaceholders: Best masked matrix (still containing 'R' placeholders)
        - int: ID of the best mask pattern (0-7)
    """
    best_score = float('inf')
    best_mask_id = -1
    best_actually_masked_matrix = None

    # Create integer-only matrix for penalty evaluation
    # 'R' and other placeholders become 0 (light) for scoring
    eval_matrix_base = []
    for r_list_placeholders in base_qr_matrix_with_placeholders:
        new_row = [val if isinstance(val, int) else 0 for val in r_list_placeholders]
        eval_matrix_base.append(new_row)

    print("\nDEBUG matrix_masking: Evaluating all 8 mask patterns (reserved areas treated as '0' for scoring):")

    # Test each mask pattern
    for p_id in range(8):
        # Apply mask to integer matrix for scoring
        current_masked_for_eval_scoring = apply_specific_mask_pattern(
            p_id,
            [row[:] for row in eval_matrix_base],  # Deep copy
            function_map
        )

        # Calculate penalty score
        score, penalties_list = calculate_total_penalty_score(current_masked_for_eval_scoring, p_id)
        print(f"  Mask {p_id}: P1={penalties_list[0]}, P2={penalties_list[1]}, "
              f"P3={penalties_list[2]}, P4={penalties_list[3]} => Total: {score}")

        # Track best score and corresponding mask
        if score < best_score:
            best_score = score
            best_mask_id = p_id
            # Apply mask to original matrix (preserving 'R' placeholders)
            best_actually_masked_matrix = apply_specific_mask_pattern(
                p_id,
                [row[:] for row in base_qr_matrix_with_placeholders],
                function_map
            )

    # Fallback to mask 0 if no best found (shouldn't happen)
    if best_actually_masked_matrix is None:
        print("Warning: Defaulting to mask 0 as no score improvement was found. Initial matrix may have issues.")
        best_mask_id = 0
        best_actually_masked_matrix = apply_specific_mask_pattern(
            0, [row[:] for row in base_qr_matrix_with_placeholders], function_map
        )

    print(f"DEBUG matrix_masking: Selected Mask ID: {best_mask_id} with Best Penalty Score: {best_score}")

    return best_actually_masked_matrix, best_mask_id

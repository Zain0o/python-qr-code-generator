"""
QR Code Byte Mode Encoder
-------------------------

This program encodes a user-provided URL into a QR code using the Byte Mode encoding scheme,
following the QR Code standard. The process includes:

1. Validating the input to ensure it is a well-formed URL.
2. Encoding the URL into a sequence of 8-bit codewords (Byte Mode).
3. Generating error correction codewords using Reed-Solomon coding.
4. Displaying the data codewords, error correction codewords, and the complete codeword sequence.

Authors:
    - Ahmed Elamari (Input Validation, Byte Mode encoding implementation)
    - Nolan Picardo (Error codewords V1 and V2)

This script is intended for educational and demonstration purposes, and is written to professional
documentation and code standards.

"""
import re
from error_correction import generate_error_correction  # Changed to use new module


def is_valid_url(text: str) -> bool:
    """Check if the input is a valid URL.
    Args:
        text (str): The input text to check.
    Returns:
        bool: True if the input is a valid URL, False otherwise.
    """
    pattern = re.compile(
        r'^(https?://)?'          # http:// or https:// (optional)
        r'([\da-z\.-]+)\.([a-z\.]{2,6})'  # domain name
        r'([/\w\.-]*)*/?$'        # optional path
    )
    return bool(pattern.match(text))


# Ahmed's code
def encode_byte_mode(data: str) -> tuple[list[str], int]:
    """Return list of 8-bit codewords and selected version for QR code in Byte Mode.
    Args:
        data (str): The input data to encode.
    Returns:
        tuple[list[str], int]: A tuple containing the list of 8-bit codewords and the selected version.
    Raises:
        ValueError: If the input data contains characters outside ISO-8859-1 or is too long.
    """
    # Convert to ISO-8859-1 (Latin-1) encoding bytes
    try:
        data_bytes = data.encode('iso-8859-1', errors='strict')
    except UnicodeEncodeError:
        raise ValueError("Data contains characters outside ISO-8859-1.")

    # Version capacity check (data capacity only)
    max_data_bytes_v1_l = 17  # Version 1-L data capacity
    max_data_bytes_v2_l = 32  # Version 2-L data capacity

    # Determine version based on data length
    if len(data_bytes) <= max_data_bytes_v1_l:
        version = 1
        total_data_codewords_for_version = 19
    elif len(data_bytes) <= max_data_bytes_v2_l:
        version = 2
        total_data_codewords_for_version = 34
    else:
        raise ValueError(f"Input too long for Version 2 L (max {max_data_bytes_v2_l} bytes).")

    total_bits_capacity_for_data_codewords = total_data_codewords_for_version * 8

    # Mode indicator (4 bits) - 0100 for Byte mode
    bit_stream = '0100'

    # Character count (8 bits for both V1 and V2 in Byte mode)
    bit_stream += format(len(data_bytes), '08b')

    # Data bytes
    for byte_val in data_bytes:
        bit_stream += format(byte_val, '08b')

    # Terminator (up to 4 zeros, but don't exceed total_bits_capacity_for_data_codewords)
    remaining_bits = total_bits_capacity_for_data_codewords - len(bit_stream)
    terminator_bits = min(4, remaining_bits)
    bit_stream += '0' * terminator_bits

    # Pad to byte boundary
    while len(bit_stream) % 8 != 0 and len(bit_stream) < total_bits_capacity_for_data_codewords:
        bit_stream += '0'

    # Pad with alternating pad codewords until full
    pad_bytes = ['11101100', '00010001']  # QR spec pad bytes
    pad_index = 0
    while len(bit_stream) < total_bits_capacity_for_data_codewords:
        remaining = total_bits_capacity_for_data_codewords - len(bit_stream)
        if remaining >= 8:
            bit_stream += pad_bytes[pad_index % 2]
            pad_index += 1
        else:
            # This shouldn't happen if logic is correct, but safety check
            bit_stream += '0' * remaining
            break

    # Split into 8-bit codewords
    data_codewords_binary_strings = [
        bit_stream[i:i + 8] for i in range(0, len(bit_stream), 8)
    ]

    # Ensure we have exactly the right number of codewords
    if len(data_codewords_binary_strings) != total_data_codewords_for_version:
        print(f"Warning: Expected {total_data_codewords_for_version} codewords, "
              f"got {len(data_codewords_binary_strings)}")
        data_codewords_binary_strings = data_codewords_binary_strings[:total_data_codewords_for_version]
        while len(data_codewords_binary_strings) < total_data_codewords_for_version:
            data_codewords_binary_strings.append('00000000')

    return data_codewords_binary_strings, version

    # Split into 8-bit codewords
    return [bits[i:i + 8] for i in range(0, total_data_bits, 8)], version

# Nolan's code
def main():
    user_input_text = input("Enter text (e.g., URL or message): ").strip()
    if not is_valid_url(user_input_text):
        print("Not a valid URL.")
        return

    try:
        # Get the data codewords and version
        data_codewords_binary, version = encode_byte_mode(user_input_text)  # Modified to capture version
        print(f"\nUsing QR Version {version}")  # Added version display

        # Then generate the error correction codes
        data_codewords_integers = [int(cw_bin, 2) for cw_bin in data_codewords_binary]
        ecc_codewords_binary = generate_error_correction(data_codewords_integers, version)  # Added version parameter

    except ValueError as e:
        print(f"Error: {e}")
        return

    # Determine expected counts based on version
    if version == 1:
        data_count = 19
        ecc_count = 7
    else:
        data_count = 34
        ecc_count = 10

    # Show results
    print(f"\nData codewords ({data_count}):")
    for i, cw_bin in enumerate(data_codewords_binary[:data_count], start=1):  # Added slicing for safety
        print(f"{i:2d}: {cw_bin} (0x{int(cw_bin, 2):02X})")

    print(f"\nError correction codewords ({ecc_count}):")
    for i, cw_bin in enumerate(ecc_codewords_binary, start=1):
        print(f"{i:2d}: {cw_bin} (0x{int(cw_bin, 2):02X})")

    print(f"\nAll codewords ({data_count + ecc_count} total):")
    all_codewords_binary = data_codewords_binary[:data_count] + ecc_codewords_binary  # Added slicing here as well
    for i, cw_bin in enumerate(all_codewords_binary, start=1):
        print(f"{i:2d}: {cw_bin} (0x{int(cw_bin, 2):02X})")

if __name__ == "__main__":
    main()

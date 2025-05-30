"""
QR Code Byte Mode Encoder
-------------------------

This program encodes a user-provided URL into a QR code using the Byte Mode encoding scheme,
following the QR Code standard. The process includes:

1. Validating the input to ensure it is a well-formed URL.
2. Encoding the URL into a sequence of 8-bit codewords (Byte Mode).
3. Generating error correction codewords using Reed-Solomon coding.
4. Displaying the data codewords, error correction codewords, and the complete codeword sequence.

Author: Zain Alshammari
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
        r'^(https?://)?'  # http:// or https:// (optional)
        r'([\da-z\.-]+)\.([a-z\.]{2,6})'  # domain name
        r'([/\w\.-]*)*/?$'  # optional path
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
        ValueError: If the input data contains characters is too long.
    """

    # Encode input data to ISO-8859-1 bytes, raising ValueError for unsupported characters
    try:
        data_bytes = data.encode('iso-8859-1', errors='strict')
    except UnicodeEncodeError:
        raise ValueError("Data contains characters outside ISO-8859-1.")

    # Version capacity check
    if len(data_bytes) <= 17:
        version = 1
        total_data_codewords = 19
    elif len(data_bytes) <= 32:
        version = 2
        total_data_codewords = 34
    else:
        raise ValueError(f"Input too long for Version 2 L (max 32 bytes).")

    # Build bit stream
    bit_stream = '0100'  # Byte mode indicator
    bit_stream += format(len(data_bytes), '08b')  # Character count (8 bits for V1 & V2)

    # Add data bytes
    for byte in data_bytes:
        bit_stream += format(byte, '08b') 

    # Calculate total bits needed
    total_bits_needed = total_data_codewords * 8

    # Add terminator (up to 4 zeros)
    remaining_bits = total_bits_needed - len(bit_stream)
    terminator_bits = min(4, remaining_bits)
    bit_stream += '0' * terminator_bits

    # Pad to byte boundary
    if len(bit_stream) % 8 != 0:
        bit_stream += '0' * (8 - len(bit_stream) % 8) 

    # Add pad bytes
    pad_bytes = ['11101100', '00010001']
    pad_index = 0
    while len(bit_stream) < total_bits_needed:
        bit_stream += pad_bytes[pad_index % 2]
        pad_index += 1

    # Split into codewords
    codewords = [bit_stream[i:i + 8] for i in range(0, len(bit_stream), 8)]
    return codewords[:total_data_codewords], version


def main():
    # Prompt user for input and remove leading/trailing whitespace
    user_input_text = input("Enter text (e.g., URL or message): ").strip()
    
    # Validate URL format before proceeding
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
 
    # Define codeword counts based on QR code version
    # Version 1: 19 data codewords, 7 error correction codewords
    # Version 2+: 34 data codewords, 10 error correction codewords
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

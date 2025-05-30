""""
QR Code Error Correction Module

This module handles error correction for QR codes using Reed-Solomon coding,
supporting both Version 1 and Version 2 QR codes at ECC Level L.

For Version 1:
- 19 data codewords
- 7 error correction codewords
- 26 total codewords

For Version 2:
- 34 data codewords
- 10 error correction codewords
- 44 total codewords

Author: Nolan Picardo
Student ID: 32019494
"""

from reedsolo import RSCodec
from typing import List  # Import List for type hinting


def generate_error_correction(data_codeword_integers: List[int], version: int) -> List[str]:
    """
    Adds error correction to the QR code data for either Version 1 or Version 2.

    Args:
        data_codeword_integers (List[int]): List of integers representing data codewords.
        version (int): QR code version (1 or 2)

    Returns:
        List[str]: List of binary strings representing error correction codewords.

    Raises:
        ValueError: If an unsupported version is provided.
    """
    # Determine error correction parameters based on version
    if version == 1:
        ecc_count = 7  # Number of ECC codewords for V1-L
    elif version == 2:
        ecc_count = 10  # Number of ECC codewords for V2-L
    else:
        raise ValueError(f"Unsupported QR version for error correction: {version}")

    # Change binary strings to numbers  (already integers here)
    # data_codeword_integers is already a list of integers
    # No need to convert from binary strings in this implementation.

    # Set up Reed-Solomon with appropriate error-correction codewords
    rs = RSCodec(ecc_count)  # Use ecc_count derived above

    # reedsolo.encode expects a sequence of integer “bytes”
    try:
        full_message_integers = rs.encode(data_codeword_integers)
    except Exception as e:
        print(f"Error during reedsolo.encode: {e}")
        print(f"Data passed to reedsolo: {data_codeword_integers}")
        raise

    # The last N values are our error-correction bytes (N = ecc_count)
    ecc_byte_integers = full_message_integers[-ecc_count:]

    # Convert error-correction bytes back to 8-bit binary strings
    ecc_binary_strings = [format(b, '08b') for b in ecc_byte_integers]

    return ecc_binary_strings

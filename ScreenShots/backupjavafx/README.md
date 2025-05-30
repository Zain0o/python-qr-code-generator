# CS2PP-QR
### Operation Instructions

The QR code pipeline is modular. Each stage can be used independently or as part of the complete QR code generation process.

#### 1. Input Validation & Data Encoding

- Import functions:
  ```python
  from data_encoding import encode_byte_mode, is_valid_url
  ```

- Validate user input:
  ```python
  url = "https://rdg.ac.uk"
  if not is_valid_url(url):
      print("Invalid URL")
      exit()
  ```

- Encode input to codewords:
  ```python
  codewords, version = encode_byte_mode(url)
  print(f"Version: {version}")
  print(f"Codewords: {codewords}")
  # codewords: List of 8-bit binary strings, version: QR version used (1 or 2)
  ```

#### 2. Error Correction

- Generate error correction codewords using Reed-Solomon:
  ```python
  from error_correction import generate_error_correction

  ecc_codewords = generate_error_correction(codewords, version)
  print(f"ECC: {ecc_codewords}")
  # ecc_codewords: List of 8-bit binary strings (number depends on version)
  ```

#### 3. Data Bitstream Assembly

- Concatenate data codewords and error correction codewords into a bitstring for matrix placement:
  ```python
  data_bits = ''.join(codewords + ecc_codewords)
  ```

#### 4. QR Matrix Construction

- Build the base QR matrix with finder/timing patterns and reserved areas:
  ```python
  import matrix_layout

  matrix = matrix_layout.generate_qr_module(data_bits)
  # matrix: SIZE x SIZE matrix (lists of 0/1)
  ```

#### 5. Masking

- Apply a masking pattern to the matrix to increase scannability:
  ```python
  import matrix_masking

  # Generate function pattern mask (areas not to mask)
  function_pattern_matrix = matrix_masking.create_function_pattern_matrix(version=version)

  # Apply mask pattern 0 (example)
  masked_matrix = matrix_masking.apply_mask_pattern_0(matrix, function_pattern_matrix)
  ```

#### 6. Format Information

- Compute the format string for ECC and mask (e.g., Error Correction Level 'L' and Mask Pattern 0):
  ```python
  format_str = matrix_layout.get_format_string(ecc_level='01', mask_pattern='000')
  ```

- Place format information into the masked matrix:
  ```python
  final_matrix = matrix_layout.place_format_information(masked_matrix, format_str)
  ```

#### 7. Rendering/Display

- Render or print the completed QR matrix:
  ```python
  matrix_layout.print_matrix(final_matrix)
  ```

---

**Pipeline Summary**  
1. Validate input (URL/text).
2. Encode data (byte mode) and select version.
3. Generate error correction codewords.
4. Assemble data+ECC bits into a bitstring.
5. Construct base QR matrix and insert fixed patterns.
6. Apply masking pattern.
7. Insert format information.
8. Render or export QR code as desired.

---

**Dependencies:**  
- `reedsolo` for error correction  
- All pipeline modules (`data_encoding`, `error_correction`, `matrix_layout`, `matrix_masking`) must be available in your environment.

---

## 2. Programming Paradigms Used

- **Imperative:**  
  - Direct input validation, error handling, and step-by-step logic throughout the pipeline, including data encoding, error correction, matrix construction, masking, and format information insertion.
  - Each stage (validation, encoding, ECC, matrix assembly, masking) is sequenced with explicit control flow.

- **Functional:**  
  - Core logic for each pipeline stage is encapsulated in pure functions:
    - `is_valid_url`, `encode_byte_mode` (supports Version 1 and Version 2 QR Code Byte Mode encoding)
    - `generate_error_correction` (error correction codeword generation, parameterized by version)
    - `generate_qr_module`, `place_data_bits`, `get_format_string`, `place_format_information` (matrix construction and processing)
    - `apply_mask_pattern_0`, `create_function_pattern_matrix` (masking and function pattern computation)
  - Functions take inputs and return outputs without side effects, supporting testability and composability.

- **Object-Oriented:**  
  - The pipeline is designed for modularity, with each functional stage easily integrable as a class method or as part of a larger object-oriented system.
  - Matrices and QR components can be encapsulated in objects if extended, enabling future expansion to class-based architectures (e.g., QRCodeMatrix, QRCodeEncoder).

- **Modularity:**  
  - Each processing stage (validation, encoding, error correction, matrix construction, masking) is a distinct module or function, supporting separation of concerns and maintainability.

- **Extensibility:**  
  - The system supports Version 1 and Version 2 QR encoding and is designed to be extensible for additional QR versions or error correction levels.

---

## 3. Social, Legal, and Ethical Considerations

The project considers several social, legal, and ethical aspects:

- **Accessibility & Usability:**
    - The core logic is designed modularly, allowing for integration into various user interface contexts.
    - The provided Flask web application (`app.py`) offers an accessible way for users to generate QR codes without needing to run Python scripts directly.
    - Clear error messages and input feedback are provided to guide the user.

- **Legal & Standardization:**
    - **Input Sanitization & Encoding:** The system validates inputs (e.g., URL format in `is_valid_url`) and uses the standard ISO-8859-1 encoding for Byte Mode (`data_encoding.py`). This, along with data length restrictions, helps prevent misuse such as attempts at code injection or creating overly complex, non-standard QR codes.
    - **Adherence to QR Standards:** The generation process strictly follows QR Code Version 1 specifications, including:
        - Correct Byte Mode encoding, mode indicators, and character counts (`data_encoding.py`).
        - Implementation of Reed-Solomon error correction for Level L (`data_encoding.py`).
        - Accurate placement of structural elements like finder patterns, alignment patterns, timing patterns, and the dark module (`matrix_layout.py`).
        - Correct application of masking (Pattern 0) to improve scannability (`matrix_masking.py`).
        - Proper generation and placement of format information (`matrix_layout.py`).
    - **Self-Contained Logic:** The core QR generation logic is custom-built (as noted in `app.py`), which is valuable for educational transparency. For production systems, using extensively vetted and licensed libraries would typically be a consideration for robustness and compliance.

- **Ethical & Educational:**
    - **Transparency & Education:** The project is intended for educational purposes, with well-documented code (`data_encoding.py`, `app.py`, `matrix_layout.py`, `matrix_masking.py`) to make the QR code generation process transparent and understandable. The `main` function in `data_encoding.py` also demonstrates intermediate steps.
    - **Reliability:** Masking patterns are applied (`matrix_masking.py`) to enhance the scannability and reliability of the generated QR codes, ensuring they function effectively.
    - **Responsible Input Handling:** The system validates and restricts input data (e.g., character set, length) to ensure that generated QR codes are functional and adhere to the targeted QR version's capacity, preventing the creation of invalid or misleading codes.
    - **Error Management:** The system includes error handling (e.g., `ValueError` for invalid data in `data_encoding.py`, error displays in `app.py`) to manage unexpected situations gracefully.

---

## 4. Known Weaknesses and Flaws

### Technical Limitations
- **Version Support:**
    - Limited to Version 1 and Version 2 QR codes only
    - No support for higher versions (3-40) which would allow for larger data capacity
    - No support for different error correction levels (M, Q, H) beyond Level L

- **Encoding Restrictions:**
    - Only supports ISO-8859-1 (Latin-1) encoding as per QR spec for Version 1
    - No support for other encoding modes (Numeric, Alphanumeric, Kanji)
    - No support for UTF-8 or other Unicode encodings
    - Input exceeding 17 bytes (for Version 1-L) will automatically attempt to use Version 2-L
    - Input exceeding 32 bytes (for Version 2-L) will raise an error

- **Matrix Generation:**
    - Only implements Mask Pattern 0, missing other mask patterns that might provide better scannability
    - No mask pattern evaluation to select the optimal pattern
    - Fixed matrix size (21×21) without support for larger versions

### Security Considerations
- **Input Validation:**
    - URL validation is basic and doesn't check for malicious URLs
    - No sanitization of potentially harmful content within URLs
    - No validation of URL schemes beyond http/https
    - No protection against extremely long URLs that could cause buffer issues

- **Error Handling:**
    - Limited error recovery mechanisms
    - No graceful degradation for partial failures
    - Basic error messages that might expose implementation details

### Integration Limitations
- **Pipeline Dependencies:**
    - Tight coupling between modules makes independent testing challenging
    - No clear interface definitions between pipeline stages
    - Limited error propagation between stages

- **Performance:**
    - No optimization for large-scale QR code generation
    - In-memory processing might be inefficient for batch operations
    - No caching mechanisms for frequently generated codes

### User Experience
- **Web Interface:**
    - Basic error feedback without detailed explanations
    - No preview or validation of generated QR codes
    - No option to customize QR code appearance
    - No support for downloading or sharing generated codes

### Future Improvements
- Support for higher QR versions and error correction levels
- Implementation of all mask patterns with automatic selection
- Enhanced input validation and security measures
- Better error handling and recovery mechanisms
- Performance optimizations for batch processing
- Enhanced web interface with more features and better UX
- Support for additional encoding modes and character sets

---

## 5. Data Modelling, Input Handling, and Security

### Data Encoding & Pipeline
- **Version 1 (21×21):**
    - 17 bytes max, 19 data + 7 ECC codewords
    - ISO-8859-1 encoding only
    - Automatic padding to 208 bits
    - Matrix layout: 21×21 with finder patterns
    - Mask Pattern 0 implementation

- **Version 2 (25×25):**
    - 32 bytes max, 34 data + 10 ECC codewords
    - Same encoding as Version 1
    - Automatic padding to 352 bits
    - Extended matrix layout: 25×25
    - Compatible masking system

### Input Validation & Processing
- URL format validation via regex
- Protocol (http/https) verification
- Character set validation
- Length checks for both versions
- Automatic version selection
- Pipeline stage validation:
    - Data encoding verification
    - Error correction validation
    - Matrix placement checks
    - Mask pattern application

### Security & Error Handling
- In-memory processing only
- No data persistence
- Clear error messages for:
    - Invalid URLs
    - Unsupported characters
    - Exceeded length limits
    - Encoding failures
    - Matrix generation errors
    - Masking failures
- Basic error recovery
- Version-specific error correction

### Data Integrity
- Proper codeword structure
- Reed-Solomon error correction
- Automatic bit padding
- Version-specific capacity management
- Matrix integrity checks
- Mask pattern validation

---

## 6. Error Correction Integration

This module provides Reed-Solomon error correction for QR Code Versions 1 and 2 QR Code (ECC Level L).

### Features
- Supports both Version 1 (21×21) and Version 2 (25×25) QR codes
- Automatic codeword count selection based on version
- Standards compliant implementation using `reedsolo` library


### Version Specifications
| QR Version | Data Codewords | ECC Codewords | Total |
|----|----------------|---------------|-------|
| V1 | 19             | 7             | 26    |
| V2 | 34             | 10            | 44    |

### Installation
```bash
pip install reedsolo
```

### Usage Example
```python
from error_correction import generate_error_correction
from data_encoding import encode_byte_mode

# Encode data and auto-detect version
data_codewords, version = encode_byte_mode("https://rdg.ac.uk")

# Generate error correction (supports V1-7 or V2-10 codewords)
ecc_codewords = generate_error_correction(data_codewords, version)

print(f"Version {version} ECC:", ecc_codewords)
```

- Uses `reedsolo` library to generate:
  - 7 ECC codewords for Version 1
  - 10 ECC codewords for Version 2
- Automatically combines with data codewords for final output

---

## 7. Demonstration with Test String

### Test String: `"https://rdg.ac.uk"`

#### Input Validation
```python
is_valid_url("https://rdg.ac.uk")
# Output: True
```
#### Encoding  
```python
encode_byte_mode("https://rdg.ac.uk")
# Output (example for Version 1):
(['01000001', '00010001', ...], 1) 
```
- The first element of the tuple is a list of 8-bit codewords (total 19 for Version 1-L, 34 for Version 2-L).
- The second element is the determined QR version (1 or 2).
- Handles padding and mode/length indicators automatically.

#### Error Example (Input Too Long)
```python
encode_byte_mode("A" * 35) # Input longer than V2 capacity
# Output: ValueError: Input too long for Version 2 L (max 32 bytes).
```

---

## 8. Contributions

### 8.1 Contribution (xe013680)
- Implemented robust, standards-compliant data encoding logic in `data_encoding.py`, including automatic V1/V2 data capacity selection.
- Provided clear validation and error feedback.
- Ensured seamless integration into group QR generation pipeline.

### 8.2 Contribution (xw019494)
- Implemented Reed-Solomon error correction in `error_correction.py`
- Supports both Version 1 (7 ECC codewords) and Version 2 (10 ECC codewords)
- Integrated with main encoding workflow in `data_encoding.py`
- Added automatic version display during encoding.

### 8.3 Contribution (ql024951)
- Implemented the QR Code module placement system for V1 and V2 (with automatic selection) in `matrix_layout.py`.
- This includes finder patterns, timing patterns, dark module, reserved areas for format info, zigzag placement and alignment pattern of data bits for a 21x21 or 25x25 matrix.
- Implemented the key algorithm to select the best marking pattern in 'matrix_masking.py'

### 8.4 Contribution (ot019071)
- Developed functions in `matrix_masking.py` to apply masking patterns to the V1 QR code matrix (21x21).
- Implemented `apply_mask_pattern_0` and `create_function_pattern_matrix` for Version 1.

### 8.5 Contribution (rd001832)
- Developed the Flask web application (`app.py`) for generating Version 1 QR codes.
- Created the HTML templating and request handling for the user interface.

---

## 9. Screenshot
* include this at end step.*

## 10. Web Interface Summary
<<<<<<< HEAD

Web interface was built using Flask, HTML, and CSS to allow users to generate Version 1 QR codes from text 
input (up to 17 bytes). The system processes the input locally encoding, error correcting, and rendering a scannable 
QR matrix entirely in-browser. 

### Example



<h2>Project Demo: QR Code Generation V1 (Step-by-Step)</h2>

<p>Click on each step to see the corresponding part of the process:</p>

<details>
  <summary><strong>Step 1: Initial View</strong> (Click to expand)</summary>
  <p align="center">
    <em>The QR Code Generator V1 ready for input.</em><br>
    <img src="ScreenShots/Step1QRVersion1.jpg" alt="QR Code Generator V1 - Initial empty state" width="400">
  </p>
</details>

<details>
  <summary><strong>Step 2: QR Code Generated</strong> (Click to expand)</summary>
  <p align="center">
    <em>The QR Code Generator V1 showing the generated code for "Google.com".</em><br>
    <img src="ScreenShots/Step2QRVersion1.jpg" alt="QR Code Generator showing QR for Google.com" width="400">
  </p>
</details>

<details>
  <summary><strong>Step 3: Scanning in Progress</strong> (Click to expand)</summary>
  <p align="center">
    <em>A mobile device scanning the QR code and detecting "google.com".</em><br>
    <img src="ScreenShots/Step3QRVersion1.jpg" alt="Mobile phone scanning the QR code and detecting google.com" width="300">
  </p>
</details>

<details>
  <summary><strong>Step 4: Successful Navigation</strong> (Click to expand)</summary>
  <p align="center">
    <em>Google.com successfully loaded on the mobile browser after the scan.</em><br>
    <img src="ScreenShots/Step4QRVersion1.jpg" alt="Google.com loaded on mobile browser after QR scan" width="300">
  </p>
</details>

### Customisation Features

The web interface allows users to customise how their QR code is displayed.

- **Foreground & Background Colour Selection**  
  Users can choose their preferred colours to generate QR codes.

- **Module Shape Options**  
  QR modules can be rendered as circlesmore readable appearance on different screens.

- **QR Code Size Selection**  
  Users can choose between small, medium, and large QR sizes


=======

Web interface was built using Flask, HTML, and CSS to allow users to generate Version 1 QR codes from text 
input (up to 17 bytes). The system processes the input locally encoding, error correcting, and rendering a scannable 
QR matrix entirely in-browser. 

### Example



<h2>Project Demo: QR Code Generation V1 (Step-by-Step)</h2>

<p>Click on each step to see the corresponding part of the process:</p>

<details>
  <summary><strong>Step 1: Initial View</strong> (Click to expand)</summary>
  <p align="center">
    <em>The QR Code Generator V1 ready for input.</em><br>
    <img src="ScreenShots/Step1QRVersion1.jpg" alt="QR Code Generator V1 - Initial empty state" width="400">
  </p>
</details>

<details>
  <summary><strong>Step 2: QR Code Generated</strong> (Click to expand)</summary>
  <p align="center">
    <em>The QR Code Generator V1 showing the generated code for "Google.com".</em><br>
    <img src="ScreenShots/Step2QRVersion1.jpg" alt="QR Code Generator showing QR for Google.com" width="400">
  </p>
</details>

<details>
  <summary><strong>Step 3: Scanning in Progress</strong> (Click to expand)</summary>
  <p align="center">
    <em>A mobile device scanning the QR code and detecting "google.com".</em><br>
    <img src="ScreenShots/Step3QRVersion1.jpg" alt="Mobile phone scanning the QR code and detecting google.com" width="300">
  </p>
</details>

<details>
  <summary><strong>Step 4: Successful Navigation</strong> (Click to expand)</summary>
  <p align="center">
    <em>Google.com successfully loaded on the mobile browser after the scan.</em><br>
    <img src="ScreenShots/Step4QRVersion1.jpg" alt="Google.com loaded on mobile browser after QR scan" width="300">
  </p>
</details>

## 11. Web Interface Enhancements

The user interface has been redesigned for improved usability:

- **Horizontal Layout**: Inputs now display side-by-side in a clean/responsive grid.
- **Customisation Options**:
  - Foreground/Background color pickers
  - Module shape (Square or Circle)
  - QR code size (Small, Medium, Large)
  - Frame style (None, Thin, Thick)
  - Visual filters (None, Grayscale, High Contrast)
  
- **Step-by-Step View**: Optional checkbox to display each QR generation stage visually.

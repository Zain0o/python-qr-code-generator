## Individual Reflection 

**Name:** `Zain Alshammari`

**Student ID:** `32019071`

---

My allocated module responsibility focused on implementing and optimising user interface and backend QR code functionality, specifically the matrix_masking.py module together with front-end relevant code. For V1, I focused on improving QR code scannability, this required coding and testing mask pattern 0, I further added important data protection warnings and scannability guidance incorporated into templates/index.html, while also adding a clear button to enhance usability. I optimised the matrix_to_html function in app.py to support QR readability through enlarging the QR code and inserting quiet zones - without these modifications the QR codes failed to scan entirely. Continuing from this point for feature upgrades, I built upon this by implementing functional code to support all eight QR mask patterns (0-7), developing specific support functions (e.g., should_mask_pattern_4 using math.floor), moreover I assisted in implementing code to compute the four standard penalty scores. This required thorough reading of Thonky documentation to ensure QR standard consistency. A primary issue was resolving V1 scanning errors on screen display, resolved by adding quiet zones and debugging. If approaching this again, I'd test each mask pattern incrementally to detect bugs faster. This project enhanced my collaborative communication skills and demonstrated how structured coding effectively manages complex logic problems.

**Word Count: 200/200**
Name: Nolan Picardo

Student ID: 32019494

My responsibility in the project was developing the error correction module for generating QR codes, located in error_correction.py. I developed the generate_error_correction() function, which uses the reedsolo library to generate Reed-Solomon error codewords for Version 1 and Version 2 QR codes at Level L. Such codewords allow the QR code to be readable even if partially damaged or obstructed.

To ensure proper functioning, I ensured the conversion of binary data codewords into integers as the library requires and returned the result in the form of binary strings to be added into the final QR matrix. I also added checks to handle unsupported versions and potential input issues.
Consistency in format among data types when debugging encoding errors was one of my primary challenges. Careful testing and good error reporting made the module operate as intended across different versions of QR.

This project has deepened my understanding of how error correction increases real-world useability. It improved my skills in creating modular, reliable code, improved my capacity to work in a group setting, and become more comfortable using Git for version control and collaboration.
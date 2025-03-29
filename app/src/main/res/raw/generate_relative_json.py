import json

def convert_to_absolute(pages_path, quran_text_path, output_path):
    # Load the Quran text to count total lines
    with open(quran_text_path, 'r', encoding='utf-8') as f:
        quran_lines = [line.strip() for line in f if line.strip()]
    
    total_lines = len(quran_lines)
    print(f"Total Quran lines: {total_lines}")

    # Load the existing pages.json
    with open(pages_path, 'r', encoding='utf-8') as f:
        pages = json.load(f)
    
    # Process each page to convert to absolute line numbers
    current_line = 0
    new_pages = []
    
    for page in pages:
        new_page = []
        # Each page is a list of lists containing surah ranges
        for surah_ranges in page:
            new_surah_ranges = []
            for surah_range in surah_ranges:
                surah = surah_range['surah']
                start_relative = surah_range['start']
                end_relative = surah_range['end']
                
                # Calculate absolute positions (1-based)
                start_absolute = current_line + 1
                end_absolute = current_line + (end_relative - start_relative + 1)
                
                new_surah_ranges.append({
                    'surah': surah,
                    'start': start_absolute,
                    'end': end_absolute
                })
                
                current_line = end_absolute
            
            new_page.append(new_surah_ranges)
        
        new_pages.append(new_page)
    
    # Save the converted file
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(new_pages, f, indent=2, ensure_ascii=False)
    
    print(f"Converted file saved to {output_path}")
    print(f"Final line number: {current_line} (should match total lines: {total_lines})")

# Usage:
convert_to_absolute(
    pages_path='pages.json',
    quran_text_path='quran_uthmani.txt',
    output_path='pages_absolute.json'
)
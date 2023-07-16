import csv
import requests


TAG_BASENAME = "rp01_rgpdiarias_v"
TAG_VERSIONS = ["0.1", "0.2", "1.0.1", "1.0.2", "1.0.3", "1.1", "1.1.1",
                "1.2", "1.2.1", "1.2.3", "1.2.4"]


def get_file_diffs(owner, repo, base_tag, head_tag):
    url = f"https://api.github.com/repos/{owner}/{repo}/compare/{base_tag}...{head_tag}"
    response = requests.get(url)
    response_json = response.json()

    for file in response_json["files"]:
        filename = file["filename"]
        if "patch" in file:
            base_tag_line_txt = ""
            head_tag_line_txt = ""
            patch_end = False
            patch_txt = file["patch"] + "\npatch_end"
            for line in patch_txt.split('\n'):
                print(line)
                # Verify if line is a base or a head tag line text
                if line.startswith('-'):
                    base_tag_line_txt += line + '\n'
                    patch_end = False
                elif line.startswith('+'):
                    head_tag_line_txt += line + '\n'
                    patch_end = False
                else:
                    patch_end = True

                if patch_end:
                    if len(base_tag_line_txt) > 0 or len(head_tag_line_txt) > 0:
                        diffs.append([filename, base_tag, head_tag, base_tag_line_txt, head_tag_line_txt])
                    base_tag_line_txt = ""
                    head_tag_line_txt = ""

    return diffs


def write_diffs_to_csv(diffs, csv_file):
    with open(csv_file, "w", newline="") as file:
        writer = csv.writer(file)
        for diff in diffs:
            writer.writerow(diff)


if __name__ == '__main__':
    owner = "diegoquirino"
    repo = "openscience"
    diffs = [["filename", "base_tag", "head_tag", "base_tag_patch", "head_tag_patch"]]
    for i in range(0, len(TAG_VERSIONS)-1):
        base_tag = TAG_BASENAME + TAG_VERSIONS[i]
        head_tag = TAG_BASENAME + TAG_VERSIONS[i+1]
        # Fetch diffs
        diffs = get_file_diffs(owner, repo, base_tag, head_tag)
    # Write diffs to named csv files
    write_diffs_to_csv(diffs, "diffs.csv")

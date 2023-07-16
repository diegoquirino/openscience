import math
import re

import pandas as pd
from strsimpy.normalized_levenshtein import NormalizedLevenshtein

BASE_FILE = "rgp_diarias_diffs.CSV"


def print_static_summary(title, new, obs, reu):
    total = new + obs + reu
    print("\n\n" + title + ":")
    print("1) NEW:      " + str(new) + " (" + str(math.ceil(new / total * 10000) / 100) + "%)")
    print("2) OBSOLETE: " + str(obs) + " (" + str(math.ceil(obs / total * 10000) / 100) + "%)")
    print("3) REUSABLE: " + str(reu) + " (" + str(math.ceil(reu / total * 10000) / 100) + "%)")
    print("TOTAL:       " + str(total) + " (100%)")


if __name__ == '__main__':
    # Auxiliary variables for Calculations of Precision, Accuracy and Recall
    TP = 0  # Number of classified cases: (DF) low impact  == low impact (Manual)
    TN = 0  # Number of classified cases: (DF) high impact == high impact (Manual)
    FP = 0  # Number of classified cases: (DF) low impact  <> high impact (Manual)
    FN = 0  # Number of classified cases: (DF) high impact <> low impact (Manual)

    # Auxiliary variables for counting MBT Test Cases
    TC_NEW = 0  # SUM (total number) of ORIGIN new test cases
    TC_OBS = 0  # SUM (total number) of ORIGIN obsolete test cases
    TC_REU = 0  # SUM (total number) of ORIGIN reusable test cases
    TC_NEW_LEV_HUM_LOW = 0  # SUM (total number) of Levenshtein == Human consider new test cases
    TC_OBS_LEV_HUM_LOW = 0  # SUM (total number) of Levenshtein == Human consider obsolete test cases
    TC_REU_LEV_HUM_LOW = 0  # SUM (total number) of Levenshtein == Human consider reusable test cases
    TC_NEW_LEV_HUM_HIGH = 0  # SUM (total number) of Levenshtein == Human consider new test cases
    TC_OBS_LEV_HUM_HIGH = 0  # SUM (total number) of Levenshtein == Human consider obsolete test cases
    TC_REU_LEV_HUM_HIGH = 0  # SUM (total number) of Levenshtein == Human consider reusable test cases

    # For each line of csv file perform string distance function calculation
    # comparing between base_tag_patch and head_tag_patch and
    # save distance result to a new column in rgp_diarias_diffs.csv file
    # Calculations from: https://github.com/luozhouyang/python-string-similarity
    df = pd.read_csv(BASE_FILE, sep=";")
    for index, row in df.iterrows():
        data_filename = row['filename']
        data_base_tag = row['base_tag']
        data_head_tag = row['head_tag']
        if row['edit_classification_human'] == 0:
            data_classify = "Low Priority"
        else:
            data_classify = "High Priority"
        print(f"{index + 1}) [{data_classify}] {data_filename} - base: {data_base_tag} <> head: {data_head_tag}")
        # Algorithm: Levenshtein :: impact threshold >> 0.59
        levenshtein = NormalizedLevenshtein()
        head_tag_patch = re.sub(r'[^a-zA-Z0-9\s]', '', str(row['head_tag_patch']))
        base_tag_patch = re.sub(r'[^a-zA-Z0-9\s]', '', str(row['base_tag_patch']))
        distance = levenshtein.distance(head_tag_patch, base_tag_patch)
        distance = math.ceil(distance * 100) / 100
        print(base_tag_patch + "" + head_tag_patch)
        print("Distance:" + str(math.ceil(distance * 100) / 100) + "\n\n")
        df.loc[index, 'normalized_levenshtein'] = distance
        if distance > 0.59:
            df.loc[index, 'edit_classification_levenshtein'] = 1
            if int(row['edit_classification_human']) == 1:
                TN += 1
            else:
                FN += 1
                TC_NEW_LEV_HUM_HIGH += row['new_ct']
                TC_OBS_LEV_HUM_HIGH += row['obsolete_ct']
                TC_REU_LEV_HUM_HIGH += row['reusable_ct']
        else:
            df.loc[index, 'edit_classification_levenshtein'] = 0
            if int(row['edit_classification_human']) == 0:
                TP += 1
            else:
                FP += 1
                TC_NEW_LEV_HUM_LOW += row['new_ct']
                TC_OBS_LEV_HUM_LOW += row['obsolete_ct']
                TC_REU_LEV_HUM_LOW += row['reusable_ct']
        TC_NEW += row['new_ct']
        TC_OBS += row['obsolete_ct']
        TC_REU += row['reusable_ct']

    df.to_csv("rgp_diarias_final.csv", index=False, encoding='utf-8')

    print("Calculations of Precision, Accuracy and Recall:\n")
    print("1) Precision: " + str(math.ceil(TP / (TP + FP) * 10000) / 100) + "%")
    print("2) Recall:    " + str(math.ceil(TP / (TP + FN) * 10000) / 100) + "%")
    print("3) Accuracy:  " + str(math.ceil((TP + TN) / (TP + TN + FP + FN) * 10000) / 100) + "%")

    print_static_summary("Test Case Statistics", TC_NEW, TC_OBS, TC_REU)

    print_static_summary("Test Case Statistics NO Coincidence Levenstain != Human (HIGH IMPACTED)",
                         TC_NEW_LEV_HUM_HIGH, TC_OBS_LEV_HUM_HIGH, TC_REU_LEV_HUM_HIGH)

    print_static_summary("Test Case Statistics NO Coincidence Levenstain != Human (LOW IMPACTED)",
                         TC_NEW_LEV_HUM_LOW, TC_OBS_LEV_HUM_LOW, TC_REU_LEV_HUM_LOW)


kaggle_fb3_java
===============

Simple word match fromt the title and description is doing marginally better keeping in mind the time required for training and prediction.

I used my 5 years old laptop with 2 core and 4GB RAM.
The dataset is big enough (training file ~ 7 GB, Test file ~ 2 GB), we cannot process this in memorry.

I used to stream these data sets and put the required information from test dataset in memorry.

Steps:

Stream Test.csv  - store the hash of titles in a HashMap in memorry
Stream Train.csv - get the matched records (duplicate) from the HashMap and print the tags in the prediction file.
                    Build the Tags Map to get the top tags, build a co-occurance map to be used in prediction.
Stream Test.csv - if the record is not in the duplicate list, then process it using some hand coded logic and predict the tags.


For details of the scores please refer to /resources/Benchmark_scores.txt




Final Submission:

max prediction 5
top 500
others from less than 500, and co-occurance tags map processed decreasing order of the tag counts
Added few restrictions to the tags
Added new tags based on their co-occurances like if ios then objective-c etc

public leader board score : 0.69095 , position : 131
private leader board score : 0.69144 , position 131

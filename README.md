# bb-visualization-object-stats

Simple GoodData project crawler that goes through provided projects and searches for visualization objects that match the application criteria.  

## How to use

1. Run `gradle distZip` to build distribution version of app.

2. Unpack the app from `build/distributions` at target machine and run from Terminal: 

`./bb-visualization-object-stats-1.0/bin/bb-visualization-object-stats -h staging3.intgdc.com -u your-account@gooddata.com -p your-password -i ./file-with-project-ids-one-per-line.txt -o ./results.txt`

## Supported parameters

| Short param | Long param | Description
| ------------- | ------------- |------------- |
| -h | --hostname | hostname of the server, for example staging3.intgdc.com or secure.gooddata.com (default value)
| -u | --user | username to login to GoodData server
| -p | --password | password to login to GoodData server
| -i | --input | input file with project IDs, each line with one ID
| -o |--output | output file where matching visualization objects will be written

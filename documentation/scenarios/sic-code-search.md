## Search

This API provides the ability to search for SIC codes. There are two data sources 
that can be queried by specifying the `indexName` query parameter. The search 
mechanism can also be varied by making the first word more relevant, and by
supporting the Lucene query syntax. If the search returns no results, the search
is re-run with Fuzzy matching turned on, to try to produce results, e.g. in the
case of a simple typo.

The results can be further filtered by the industry sector, where relevant.

Paging is supported through the use of a `pageResults` size, along with specifying
which page is to be displayed.   

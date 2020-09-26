package scorer;

import ds.Document;
import ds.Query;
import utils.IndexUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract class for a scorer.
 * Needs to be extended by each specific implementation of scorers.
 */
public abstract class AScorer {

    // Map: term -> idf
    IndexUtils utils;

    // Various types of term frequencies that you will need
    String[] TFTYPES = {"title","body"};

    /**
     * Construct an abstract scorer with a map of idfs.
     * @param utils index utility functions like map of idf scores
     */
    public AScorer(IndexUtils utils) {
        this.utils = utils;
    }

    /**
     * You can implement your own function to whatever you want for debug string
     * The following is just an example to include page information in the debug string
     * The string will be forced to be 1-line and truncated to only include the first 200 characters
     */
    public String getDebugStr(Document d, Query q)
    {
        return "Pagerank: " + Integer.toString(d.page_rank);
    }

    /**
     * Score each document for each query.
     * @param d the ds.Document
     * @param q the ds.Query
     */
    public abstract double getSimScore(Document d, Query q);

    /**
     * Get frequencies for a query.
     * @param q the query to compute frequencies for
     */
    public Map<String,Double> getQueryFreqs(Query q) {

        // queryWord -> term frequency
        Map<String,Double> tfQuery = new HashMap<>();

        /*
         * TODO : Your code here
         * Compute the raw term frequencies
         * Additionally weight each of the terms using the idf value
         * of the term in the query (we use the provided text corpus to 
         * determine how many documents contain the query terms, which is stored
         * in this.idfs).
         */
        for(String queryWord : q.queryWords)
        {
            if(tfQuery.containsKey(queryWord)){
                tfQuery.put(queryWord, tfQuery.get(queryWord) + 1.0);
            }
            else{
                tfQuery.put(queryWord, 1.0);
            }
        }

        for(String queryWord : q.queryWords)
        {
            tfQuery.put(queryWord, tfQuery.get(queryWord) * Math.log10(utils.totalNumDocs()/utils.docFreq(queryWord)));
        }
        return tfQuery;
    }

    /*
     * TODO (Optional in case you want to do any preprocessing here) : Your code here
     * Include any initialization and/or parsing methods
     * that you may want to perform on the ds.Document fields
     * prior to accumulating counts.
     * See the ds.Document class in ds.Document.java to see how
     * the various fields are represented.
     */


    /**
     * Accumulate the various kinds of term frequencies
     * for the fields (title, body).
     * You can override this if you'd like, but it's likely
     * that your concrete classes will share this implementation.
     * @param d the ds.Document
     * @param q the ds.Query
     */
    public Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q) {

        // Map from tf type (field) -> queryWord -> score
        Map<String,Map<String, Double>> tfs = new HashMap<>();

        /*
         * TODO : Your code here
         * Initialize any variables needed
         */
        Map<String, Double> titleMap = new HashMap<>();
        Map<String, Double> bodyMap = new HashMap<>();

        /*
         * TODO: Your code here
         * Loop through query terms and accumulate term frequencies.
         * Note: you should do this for each type of term frequencies,
         * i.e. for each of the different fields.
         * Don't forget to lowercase the query word.
         */

        for(String queryWord : q.queryWords) {
            queryWord = queryWord.toLowerCase();
            titleMap.put(queryWord, 0.0);
            bodyMap.put(queryWord, 0.0);

            // calculate for title
            double titleCount = 0;
            String[] wordList = d.title.split(" ");
            for (String word : wordList) {
                if (word.toLowerCase().equals(queryWord)) {
                    titleCount++;
                }
            }
            titleMap.put(queryWord, titleCount);

            // calculate for body
            if (d.body_hits != null) {
                if (d.body_hits.containsKey(queryWord)) {
                    bodyMap.put(queryWord, (double) d.body_hits.get(queryWord).size());
                }
            }
        }

        tfs.put("title", titleMap);
        tfs.put("body", bodyMap);

    return tfs;
    }

}

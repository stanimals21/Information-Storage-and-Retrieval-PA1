package scorer;

import ds.Document;
import ds.Query;
import utils.IndexUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Skeleton code for the implementation of a BM25 scorer in Task 2.
 */
public class BM25Scorer extends AScorer {

    /*
     *  TODO: You will want to tune these values
     */
    double titleweight  = 0.1;
    double bodyweight = 0.1;

    // BM25-specific weights
    double btitle = 0.1;
    double bbody = 0.1;

    double k1 = 0.1;
    double pageRankLambda = 0.1;
    double pageRankLambdaPrime = 0.1;

    // query -> url -> document
    Map<Query,Map<String, Document>> queryDict;

    // BM25 data structures--feel free to modify these
    // ds.Document -> field -> length
    Map<Document,Map<String,Double>> lengths;

    // field name -> average length
    Map<String,Double> avgLengths;

    // ds.Document -> pagerank score
    Map<Document,Double> pagerankScores;

    /**
     * Construct a scorer.BM25Scorer.
     * @param utils Index utilities
     * @param queryDict a map of query to url to document
     */
    public BM25Scorer(IndexUtils utils, Map<Query,Map<String, Document>> queryDict) {
        super(utils);
        this.queryDict = queryDict;
        this.calcAverageLengths();
    }

    /**
     * Set up average lengths for BM25, also handling PageRank.
     */
    public void calcAverageLengths() {
        lengths = new HashMap<>();
        avgLengths = new HashMap<>();
        pagerankScores = new HashMap<>();

        /*
         * TODO : Your code here
         * Initialize any data structures needed, perform
         * any preprocessing you would like to do on the fields,
         * accumulate lengths of fields.
         * handle pagerank.
         */
        // populate lengths
        for(Query query : queryDict.keySet())
        {
            for(String url : queryDict.get(query).keySet())
            {
                Document currDoc = queryDict.get(query).get(url);

                // calculates pageRanks for each document
                pagerankScores.put(currDoc, Math.log10(currDoc.page_rank));

                // field -> length map
                HashMap<String, Double> fieldLenMap = new HashMap<>();

                for (String tfType : this.TFTYPES)
                {
                    /*
                     * TODO : Your code here
                     * Normalize lengths to get average lengths for
                     * each field (body, title).
                     */

                    // initialize value in averageLengths if not already populated
                    if(!avgLengths.containsKey(tfType))
                        avgLengths.put(tfType, 0.0);

                    if(tfType.equals("title") && currDoc.title != null) {
                        // calculates total title length for document
                        double title_length = currDoc.title_length;
                        fieldLenMap.put(tfType, title_length);
                        avgLengths.put(tfType, avgLengths.get(tfType) + title_length);
                    }

                    else if(tfType.equals("body")) {
                        double body_length = currDoc.body_length;
                        fieldLenMap.put(tfType, body_length);
                        avgLengths.put(tfType, avgLengths.get(tfType) + body_length);
                    }
                }
                lengths.put(currDoc, fieldLenMap);
            }

            for (String tfType : this.TFTYPES)
            {
                // avgLength = (total count for each field) / number of documents
                avgLengths.put(tfType, avgLengths.get(tfType) / lengths.size());
            }
        }
    }


    /**
     * Get the net score.
     * @param tfs the term frequencies
     * @param q the ds.Query
     * @param tfQuery
     * @param d the ds.Document
     * @return the net score
     */
    public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery, Document d) {

        double score = 0.0;

        /*
         * TODO : Your code here
         * Use equation 3 first and then equation 4 in the writeup to compute the overall score
         * of a document d for a query q.
         */

        for(String queryWord : q.queryWords)
        {
            double overallWeight = 0;
            // equation 3: overall weight for term t in document d
            overallWeight = ((tfs.get("title").get(queryWord) * titleweight) + (tfs.get("body").get(queryWord) * bodyweight));

            // equation 4:
            double idf = tfQuery.get(queryWord);
            score += (overallWeight/(k1 + overallWeight)) * idf + pageRankLambda * V_j("1", pageRankLambda, pagerankScores, d);
        }

        return score;
    }

    // for use in getNetScore
    public double V_j(String function, double pageRankLambda, Map<Document, Double> pagerankScores, Document d){
        double value = 0.0;
        switch(function)
        {
            case "1":
                value = Math.log10(pageRankLambdaPrime + pagerankScores.get(d));
                break;
            case "2": // placeholder
                break;
            case "3": // placeholder
                break;

        }
        return value;
    }

    /**
     * Do BM25 Normalization.
     * @param tfs the term frequencies
     * @param d the ds.Document
     * @param q the ds.Query
     */
    public void normalizeTFs(Map<String,Map<String, Double>> tfs, Document d, Query q) {
        /*
         * TODO : Your code here
         * Use equation 2 in the writeup to normalize the raw term frequencies
         * in fields in document d.
         */

        for(String type: tfs.keySet())
        {
            Map<String, Double> map = new HashMap<>();
            for(String queryWord : q.queryWords)
            {
//                map.put(queryWord, 0.0); // (tfs should already be initialized, don't need this)
                double rawTF = tfs.get(type).get(queryWord);
                double lenDF = lengths.get(d).get(type);
                double avgLenF = avgLengths.get(type);

                if(type.equals("body") && avgLenF != 0)
                {
                    double normalizedFreq = rawTF/((1-bbody)+bbody*(lenDF/avgLenF));
                    map.put(queryWord, normalizedFreq);
                }
                else if (type.equals("title") && avgLenF != 0)
                {
                    double normalizedFreq = rawTF/((1-btitle)+btitle*(lenDF/avgLenF));
                    map.put(queryWord, normalizedFreq);
                }
            }
            tfs.put(type, map);
        }
    }

    /**
     * Write the tuned parameters of BM25 to file.
     * Only used for grading purpose, you should NOT modify this method.
     * @param filePath the output file path.
     */
    private void writeParaValues(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            String[] names = {
                    "titleweight", "bodyweight", "btitle",
                    "bbody", "k1", "pageRankLambda", "pageRankLambdaPrime"
            };
            double[] values = {
                    this.titleweight, this.bodyweight, this.btitle,
                    this.bbody, this.k1, this.pageRankLambda,
                    this.pageRankLambdaPrime
            };
            BufferedWriter bw = new BufferedWriter(fw);
            for (int idx = 0; idx < names.length; ++ idx) {
                bw.write(names[idx] + " " + values[idx]);
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    /**
     * Get the similarity score.
     * @param d the ds.Document
     * @param q the ds.Query
     * @return the similarity score
     */
    public double getSimScore(Document d, Query q) {
        Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
        this.normalizeTFs(tfs, d, q);
        Map<String,Double> tfQuery = getQueryFreqs(q);

        // Write out the tuned BM25 parameters
        // This is only used for grading purposes.
        // You should NOT modify the writeParaValues method.
        writeParaValues("bm25Para.txt");
        return getNetScore(tfs,q,tfQuery,d);
    }

}

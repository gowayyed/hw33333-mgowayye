package edu.cmu.lti.f14.hw3.hw3_mgowayye.casconsumers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_mgowayye.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_mgowayye.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_mgowayye.utils.Utils;

/**
 * 
 * This class has two tasks:
 * 
 * 1. On the document level, it stores information to be used in the evaluation 2. After all
 * documents are processed, the collectionProcessComplete is called to perform the actual
 * evaluation.
 *
 */

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  /** query texts **/
  public ArrayList<String> textsList;

  /** query tokens **/
  public ArrayList<Map<String, Integer>> tokensList;

  /**
   * Initialization method that just initializes the lists.
   */
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    textsList = new ArrayList<String>();

    tokensList = new ArrayList<Map<String, Integer>>();
  }

  /**
   * This function works on the document level. It store information about each document to be used
   * later by the evaluator.
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {
    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }
    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
    // iterate over all the provided documents in the CAS. In our experiments, we provide only one
    // document per CAS
    if (it.hasNext()) {
      Document doc = (Document) it.next();
      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());
      textsList.add(doc.getText());
      tokensList.add(getTokensMap(Utils.fromFSListToCollection(doc.getTokenList(), Token.class)));
    }
  }

  /**
   * This function converts from an ArrayList of {@link Token} to a Map with <key, value> is
   * <tokenString, frequency>
   * 
   * @param tokens
   * @return
   */
  private Map<String, Integer> getTokensMap(ArrayList<Token> tokens) {
    Map<String, Integer> res = new HashMap<String, Integer>();
    for (Token t : tokens)
      res.put(t.getText(), t.getFrequency());
    return res;
  }

  /**
   * This function is executed after processing all documents. It first computes the cosines
   * similarities then computes the MRR metric and prints it to console. It also prints to the
   * results.txt file.
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {
    super.collectionProcessComplete(arg0);
    // make sure data is consistent
    if (tokensList.size() != relList.size() || relList.size() != qIdList.size())
      System.err.println("Error in loading the data.");
    // print the size to make sure everything is working fine
    System.out.println("SIZE = " + tokensList.size());

    // compute the cosine similarity measure
    ArrayList<ArrayList<Similarity>> cosineSimilarities = computeCosineSimilarityMap();

    // compute the metric:: mean reciprocal rank
    double metricMRR = computeMRR(cosineSimilarities);
    System.out.println(" (MRR) Mean Reciprocal Rank :: " + metricMRR);
  }

  /**
   * This function computes the cosine similarities between the queries and the documents provided
   * for each query.
   * 
   * @return {@link ArrayList of {@link ArrayList<@link Similarity>}
   */
  private ArrayList<ArrayList<Similarity>> computeCosineSimilarityMap() {
    ArrayList<ArrayList<Similarity>> cosineSimilarities = new ArrayList<ArrayList<Similarity>>();

    for (int i = 0; i < tokensList.size(); i++) {
      if (relList.get(i) == 99) // This is a query
      {
        ArrayList<Similarity> cosinesMap = new ArrayList<Similarity>();
        for (int j = 0; j < tokensList.size(); j++) {
          if ((relList.get(j) == 0 || relList.get(j) == 1) && qIdList.get(j) == qIdList.get(i)) {
            ArrayList<String> tokensUnion = getTokensUnion(tokensList.get(i), tokensList.get(j));
            double cosSim = computeCosineSimilarity(getVector(tokensList.get(i), tokensUnion),
                    getVector(tokensList.get(j), tokensUnion));
            System.out.println(qIdList.get(j) + "\t" + cosSim);
            cosinesMap.add(new Similarity(i, j, cosSim));
          }
        }
        cosineSimilarities.add(cosinesMap);
      }
    }
    return cosineSimilarities;
  }

  /**
   * This function is used in computing the cosine similarities. It computes the union of two tokens
   * list.
   * 
   * @param map
   * @param map2
   * @return
   */
  private ArrayList<String> getTokensUnion(Map<String, Integer> map, Map<String, Integer> map2) {
    ArrayList<String> res = new ArrayList<String>();
    for (String ts : map.keySet()) {
      if (!res.contains(ts)) // This if is redundant because tokens should be unique in the
                             // first place.
      {
        res.add(ts);
      }
    }
    for (String ts : map2.keySet()) {
      if (!res.contains(ts)) {
        res.add(ts);
      }
    }
    return res;
  }

  /**
   * This method create the vector as a {@link HashMap} by taking an existing vector and a union of
   * tokens.
   * 
   * @param map
   * @param tokensUnion
   * @return
   */
  private Map<String, Integer> getVector(Map<String, Integer> map, ArrayList<String> tokensUnion) {
    Map<String, Integer> vector = new HashMap<String, Integer>();

    boolean found = false;
    for (String s : tokensUnion) {
      found = false;
      for (String t : map.keySet()) {
        if (s.equals(t)) {
          vector.put(s, map.get(t));
          found = true;
        }
      }
      if (!found)
        vector.put(s, 0);
    }
    return vector;
  }

  /**
   * 
   * This method computes the cosine similarity between two vectors. Each vector is represented as a
   * {@link Map}
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double ab = 0;
    double a2 = 0;
    double b2 = 0;
    double ai = 0;
    double bi = 0;
    for (String s : queryVector.keySet()) {
      ai = queryVector.get(s);
      bi = docVector.get(s);
      ab += ai * bi;
      a2 += Math.pow(ai, 2);
      b2 += Math.pow(bi, 2);
    }
    return ab / (Math.sqrt(a2) * Math.sqrt(b2));
  }

  /**
   * 
   * This function computer the MRR mertic using the cosine similarities. MRR is the average inverse
   * of ranks of the relevant documents after sorting using cosine similarities.
   * 
   * @param cosineSimilarities
   * @return mrr
   * @throws UnsupportedEncodingException
   * @throws FileNotFoundException
   */
  private double computeMRR(ArrayList<ArrayList<Similarity>> cosineSimilarities)
          throws FileNotFoundException, UnsupportedEncodingException {
    double mrr = 0;
    PrintWriter writer = new PrintWriter("results.txt", "UTF-8"); // write the output
    for (ArrayList<Similarity> cosines : cosineSimilarities) {
      // first sort using cosine similarities
      Collections.sort(cosines);
      int aind = 1;
      for (Similarity s : cosines) {
        s.rank = aind;
        if (relList.get(s.doc2Index) == 1) {
//          writer.println(textsList.get(s.doc1Index));
          writer.println(s);
          System.out.println(s);
          mrr += (1.0 / aind);
        }
        aind++;
      }
    }
    mrr = mrr / cosineSimilarities.size();
    writer.println("MRR=" + mrr);
    writer.close();
    return mrr;
  }

  /**
   * This class represents a similarity between two documents or a query and a document. It is
   * created to facilitate sorting over cosine similarities by implementing {@link Comparable}
   * intergface, to be able to directly sort using {@link Collections} Java class.
   * 
   * @author gowayyed
   *
   */
  private class Similarity implements Comparable<Similarity> {
    /**
     * The index of the first document (the query in our case) in the ArrayLists that persist the documents data.
     * 
     */
    public int doc1Index;

    /**
     * The index of the second document in the ArrayLists that persist the documents data.
     */
    public int doc2Index;

    /**
     * The actual value of the similarity (cosine in our case)
     */
    public double similarity;

    /**
     * The rank that this similarity took among other documents for the same query.
     */
    public int rank;

    /**
     * Constructs a {@link Similarity}
     * 
     * @param doc1Index
     * @param doc2Index
     * @param similarity
     */
    public Similarity(int doc1Index, int doc2Index, double similarity) {
      super();
      this.doc1Index = doc1Index;
      this.doc2Index = doc2Index;
      this.similarity = similarity;
    }

    /**
     * Defines how to compare to similarities to be used by {@link Collections} class. It also
     * checks whether this comparison is done for the same query or not.
     */
    @Override
    public int compareTo(Similarity o) {
      if (doc1Index != o.doc1Index)
        System.err.println("Comparing two similarities for different queries!!");
      return similarity > o.similarity ? -1 : 1;
    }
    
    /**
     * This is used to print the similarity to the file using the requested format.
     */

    @Override
    public String toString() {
      return "cosine=" + new DecimalFormat("##.####").format(similarity) + "\trank=" + rank
              + "\tqid=" + qIdList.get(doc1Index) + "\trel=" + relList.get(doc2Index) + "\t"
              + textsList.get(doc2Index);
    }
  }
}

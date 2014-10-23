package edu.cmu.lti.f14.hw3.hw3_mgowayye.annotators;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_mgowayye.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_mgowayye.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_mgowayye.utils.Utils;
 
/**
 * 
 * This class takes a sentence, tokenizes it and create a token list with frequencies.
 *
 */
public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  /**
   * The process method that is executed on each sentence.
   */
  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
   * @param doc
   *          input text
   * @return a list of tokens.
   */

  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();

    for (String s : doc.split("\\s+"))
      res.add(s);
    return res;
  }

  /**
   * 
   * This function perform the tokenization and then loops over tokens to count frequencies, create list of {@link Token}s, then adds it to the cas.
   * 
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();
    ArrayList<String> allTokens = (ArrayList<String>) tokenize0(docText);
    ArrayList<Token> tokens = new ArrayList<Token>();
    boolean found = false;
    for (String s : allTokens) {
      found = false;
      for (Token t : tokens) {
        if (t.getText().equals(s)) {
          found = true;
          t.setFrequency(t.getFrequency() + 1);
        }
      }
      if (!found) {
        Token newToken = new Token(jcas);
        newToken.setText(s);
        newToken.setFrequency(1);
        tokens.add(newToken);
      }
    }
    // sets the token list in the cas
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, tokens));
  }

}

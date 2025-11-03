import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.QueryParser; 
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class SearcherLucene {

	public static void main(String[] args) throws Exception {
	    String indexPath = "./lucene-index";
	    Directory dir = FSDirectory.open(Paths.get(indexPath));

	    // apri l'indice in lettura
	    IndexReader reader = DirectoryReader.open(dir);
	    IndexSearcher searcher = new IndexSearcher(reader);

	    
	    Map<String, Analyzer> analyzerMap = new HashMap<>();
	    analyzerMap.put("nome", new StandardAnalyzer()); 
	    analyzerMap.put("contenuto", new EnglishAnalyzer()); 

	    Analyzer perFieldAnalyzer = new PerFieldAnalyzerWrapper(
	        new StandardAnalyzer(), 
	        analyzerMap           
	    );

	    //QueryParser
	    QueryParser parser = new QueryParser("contenuto", perFieldAnalyzer);

	    BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
	    
	    while (true) {
	        System.out.println("\nInserisci la query (es. contenuto:\"DEA agent\" OR nome:500) o 'exit':");
	        String line = consoleReader.readLine();

	        if (line == null || line.equalsIgnoreCase("exit")) {
	            break;
	        }
	        if (line.trim().isEmpty()) {
	            continue;
	        }

	        //query
	        Query query = parser.parse(line);
	        TopDocs hits = searcher.search(query, 10); 

	        System.out.println("Trovati " + hits.totalHits.value + " risultati totali.");
	        
	        //risultati
	        StoredFields storedFields = searcher.storedFields();

	        
	        if (hits.scoreDocs.length > 0) {
	            System.out.println("Stampo solo il risultato migliore:");

	            ScoreDoc scoreDoc = hits.scoreDocs[0];
	            
	            Document doc = storedFields.document(scoreDoc.doc);
	            System.out.printf("  Score: %f - File: %s\n",
	                    scoreDoc.score, doc.get("nome"));

	            String searchTerm = null; 

	            if (line.toLowerCase().trim().startsWith("contenuto:")) {
	                String[] queryParts = line.split(":", 2);
	                if (queryParts.length > 1) {
	                    searchTerm = queryParts[1]; 
	                }
	            } 
	            else if (!line.toLowerCase().trim().startsWith("nome:")) { 
	                searchTerm = line; 
	            }

	            if (searchTerm != null) {
	                String testoCompleto = doc.get("contenuto"); 
	                
	                if (testoCompleto != null) {
	                    String primoTermine = searchTerm.replace("\"", "").trim().split(" ")[0];
	                    
	                    if (!primoTermine.isEmpty()) {
	                        int index = testoCompleto.toLowerCase().indexOf(primoTermine.toLowerCase());
	                        
	                        if (index != -1) {
	                            int startOfLine = Math.max(0, testoCompleto.lastIndexOf('\n', index) + 1);
	                            int endOfLine = testoCompleto.indexOf('\n', index);
	                            if (endOfLine == -1 || endOfLine <= startOfLine) {
	                                endOfLine = testoCompleto.length();
	                            }

	                            String riga = testoCompleto.substring(startOfLine, endOfLine);

	                            if (riga.trim().startsWith("Titolo:")) {
	                                System.out.println("      snippet: [Trovato nel titolo, ma il contenuto rilevante non e' mostrato]");
	                            } else {
	                                System.out.println("      snippet: ... " + riga.trim() + " ...");
	                            }
	                        }
	                    }
	                }
	            }	        
	        } else {
	            System.out.println("Nessun risultato trovato.");
	        }
	    }
	    reader.close();
	}
}
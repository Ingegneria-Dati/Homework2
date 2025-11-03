import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger; 
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class IndexerLucene {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    public static void main(String[] args) throws IOException {
        
        String indexPath = "./lucene-index";
        String docsPath = "C:\\Users\\veryf\\INGEGNERIA\\Magistrale\\2°anno\\Ing.Dati\\Homework2\\movies_txt"; // Percorso dei file

        Path path = Paths.get(indexPath);
        Directory directory = FSDirectory.open(path);

        //indice
        Map<String, Analyzer> analyzerMap = new HashMap<>();
        analyzerMap.put("nome", new StandardAnalyzer());
        analyzerMap.put("contenuto", new EnglishAnalyzer());
        Analyzer perFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerMap);

        IndexWriterConfig iwc = new IndexWriterConfig(perFieldAnalyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE); 
        IndexWriter writer = new IndexWriter(directory, iwc);

        
        final AtomicInteger fileCounter = new AtomicInteger(0);
        final long startTime = System.currentTimeMillis();

        System.out.println("Indicizzazione avviata in: " + docsPath);

       
        Files.walkFileTree(Paths.get(docsPath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".txt")) {
                    indexDoc(writer, file);
                    fileCounter.incrementAndGet(); 
                }
                return FileVisitResult.CONTINUE;
            }
        });

        writer.commit();
        writer.close();

        //metriche
        final long endTime = System.currentTimeMillis();
        final long durationMs = endTime - startTime;
        final int totalFiles = fileCounter.get();
        final double durationSec = durationMs / 1000.0;
        final double filesPerSec = totalFiles / durationSec;
        final double avgTimePerFile = (double) durationMs / totalFiles;

        long indexSizeBytes = 0;
        try (Stream<Path> stream = Files.walk(path)) {
            indexSizeBytes = stream.filter(Files::isRegularFile)
                                  .mapToLong(p -> { try { return Files.size(p); } catch (IOException e) { return 0; } })
                                  .sum();
        }
        final double indexSizeMB = indexSizeBytes / (1024.0 * 1024.0);
        
        System.out.println("--- Indicizzazione Completata ---");
        System.out.printf("Numero di file indicizzati: %d\n", totalFiles);
        System.out.printf("Tempo di indicizzazione totale: %.2f secondi (%d ms)\n", durationSec, durationMs);
        System.out.printf("Dimensione finale dell'indice: %.2f MB\n", indexSizeMB);
        System.out.println("--- Performance ---");
        System.out.printf("Throughput: %.2f file/secondo\n", filesPerSec);
        System.out.printf("Tempo medio per file: %.2f ms/file\n", avgTimePerFile);
    }

    private static void indexDoc(IndexWriter writer, Path file) throws IOException {
        String fileName = file.getFileName().toString();
        
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        String cleanedContent = HTML_TAG_PATTERN.matcher(content).replaceAll(" ");

        Document doc = new Document();
        doc.add(new TextField("nome", fileName, Field.Store.YES));
        doc.add(new TextField("contenuto", cleanedContent, Field.Store.YES)); // Store.YES per gli snippet

        writer.addDocument(doc);
    }
}
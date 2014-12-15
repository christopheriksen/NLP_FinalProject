import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import java.io.*;
import java.util.ArrayList;
import java.util.*;

/**
 * This terminal application creates an Apache Lucene index in a folder and adds files into this index
 * based on the input of the user.
 */
public class LuceneBeta {
    private static StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);

    private IndexWriter writer;
    private ArrayList<File> queue = new ArrayList<File>();
    private String indexLocation = null;
    /*
    *  Create an index of all of our documents.  Document are named with an integer identifier
    * in ascending order.
    */
    public LuceneBeta(String inputDir, String outputDir, int context){      
        LuceneBeta indexer = null;
        try {
            indexLocation = outputDir;
            indexer = new LuceneBeta(outputDir);
        } catch (Exception ex) {
            System.out.println("Cannot create index..." + ex.getMessage());
            System.exit(-1);
        }

        //===================================================
        //read input from user until he enters q for quit
        //===================================================
            try {
                //add files to the index
                indexer.addDocuments(inputDir, context);
                //===================================================
                //after adding, we always have to call the
                //closeIndex, otherwise the index is not created
                //===================================================
                indexer.closeIndex();
            } catch (Exception e) {
                System.out.println("Error indexing " + inputDir + " : " + e.getMessage());
        }

    }
    public LuceneBeta(String inputDir, String outputDir){
        this(inputDir, outputDir, 1);
    }

   /* 
    *  Returns a query based on a Lucene indexed database and a query string.  See
    *  the Lucene documentation for query syntax.
    *
    */
    public ArrayList<String[]> queryResults(String query, int numResults, int window){

            ArrayList<String[]> results = new ArrayList<String[]>();
            String[] contextSentences = null;
            try {
                IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexLocation)));
                IndexSearcher searcher = new IndexSearcher(reader);
                TopScoreDocCollector collector = TopScoreDocCollector.create(numResults, true);
                Query q = new QueryParser(Version.LUCENE_40, "index", analyzer).parse(query);
                searcher.search(q, collector);
                ScoreDoc[] hits = collector.topDocs().scoreDocs;
                // 4. display results
                System.out.println("Found " + hits.length + " hits.");
                for(int i=0;i<hits.length;++i) {
                    int docId = hits[i].doc;
                    contextSentences = new String[window*2 + 1];
                    for(int j = 0; j < window*2 + 1; ++j){
                        Document d = searcher.doc(docId + (j - window));
                        contextSentences[j] = d.get("contents");
                    }
                    results.add(contextSentences);

                    // System.out.println((i + 1) + ". " + d.get("contents") + " score=" + hits[i].score);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error searching " + query + " : " + e.getMessage());
        }
        return results;
    }
    /**
     * Constructor
     * @param indexDir the name of the folder in which the index should be created
     * @throws java.io.IOException when exception creating index.
     */
    LuceneBeta(String indexDir) throws IOException {
        // the boolean true parameter means to create a new index everytime,
        // potentially overwriting any existing files there.
        FSDirectory dir = FSDirectory.open(new File(indexDir));
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
        writer = new IndexWriter(dir, config);
    }

    

    /**
     * Indexes a file or directory
     * @param fileName the name of a text file or a folder we wish to add to the index
     * @throws java.io.IOException when exception
     */
    public void addDocuments(String fileName, int context) throws IOException {
        
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = "";
        int docNum = 1;
        File f = null;
        String[] sentenceContexts = new String[context];
        try{
            f = new File(fileName);
        }

        catch(Exception e){
            System.out.println("Trouble reading " + fileName);
        }
        String indexContent = "";
        while((line = reader.readLine()) != null){
            try {
                sentenceContexts[docNum % context] = line;
                indexContent = "";
                for(String s : sentenceContexts){
                    if(s != null){
                        indexContent += " " + s;
                    }
                }
                Document doc = new Document();
                //===================================================
                // add contents of file
                //===================================================
                doc.add(new TextField("index", indexContent, Field.Store.NO));
                doc.add(new StringField("contents", line, Field.Store.YES));

                //  doc.add(new StringField("path", f.getPath(), Field.Store.YES));
                doc.add(new StringField("filename", docNum+"", Field.Store.YES));
                docNum++;
                writer.addDocument(doc);
                System.out.println("Added: " + indexContent);
            } catch (Exception e) {

                // System.out.println("Could not add: " + line);

                System.out.println("Could not add: " + line);

            } 
        }
        writer.close();
        System.out.println("");
        System.out.println("************************");
        System.out.println(docNum + " documents added.");
        System.out.println("************************");
    }
    public void addDocuments(String fileName) throws IOException {
        // only one sentence context
        // larger integers increase the number of prior sentences to be included
        addDocuments(fileName, 1);
    }
    /**
    *  Adds files to a queue to be indexed.  This function is not used in the case
    *  that we are indexing a single file split by sentences
    **/
    private void addFiles(File file) {

        if (!file.exists()) {
            System.out.println(file + " does not exist.");
        }
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                addFiles(f);
            }
        } else {
            String filename = file.getName().toLowerCase();
            //===================================================
            // Only index text files
            //===================================================
            if (filename.endsWith(".htm") || filename.endsWith(".html") ||
                    filename.endsWith(".xml") || filename.endsWith(".txt")) {
                queue.add(file);
            } else {
                System.out.println("Skipped " + filename);
            }
        }
    }

    /**
     * Close the index.
     * @throws java.io.IOException when exception closing
     */
    public void closeIndex() throws IOException {
        writer.close();
    }

    public static void main(String[] args){
        // 0 - index file
        // 1 - output folder
        // 2 - index context (number of preceding sentences to index)
        // 3 - number of results
        // 4 - query context (preceding and following sentences to return)
        System.out.println("It compiled!");
        LuceneBeta engine = new LuceneBeta(args[0], args[1], Integer.parseInt(args[2]));
        Scanner sc = new Scanner(System.in);
        String s = "";
        ArrayList<String[]> queryResults = null;
        while((s = sc.nextLine()) != "quit\n") {
           queryResults = engine.queryResults(s, Integer.parseInt(args[3]), Integer.parseInt(args[4]));
           System.out.println("Input: " + s);
           for(String[] s2 : queryResults) {
                System.out.println("Previous: " + s2[Integer.parseInt(args[4]) - 1]);  // the previous sentence
                System.out.println("Match: " + s2[Integer.parseInt(args[4])]);  // the matched sentene
                System.out.println("Next: " + s2[Integer.parseInt(args[4]) + 1]);  // the following sentence
           } 
        }
    }
}
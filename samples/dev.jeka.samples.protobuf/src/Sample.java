public class Sample {

    public static void main(String[] args) {
        SearchRequestOuterClassg ar.SearchRequest searchRequest = SearchRequestOuterClass.SearchRequest.newBuilder()
                .setQuery("foo")
                .setPageNumber(12)
                .build();
        System.out.println(searchRequest);
    }
}

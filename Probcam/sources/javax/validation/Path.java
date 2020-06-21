package javax.validation;

public interface Path extends Iterable<Node> {

    public interface Node {
        Integer getIndex();

        Object getKey();

        String getName();

        boolean isInIterable();
    }
}

package io.github.khezyapp.datamasker.strategy;

import io.github.khezyapp.datamasker.api.SensitiveMaskerContext;
import io.github.khezyapp.datamasker.api.SensitiveMaskerStrategy;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A sensitive data masking strategy specifically designed for collections and arrays.
 * <p>
 * This strategy iterates through the elements of a collection or array and delegates
 * the masking logic for complex items to the {@link SensitiveMaskerContext}. Primitive
 * types and null values are preserved in the output to maintain the structure of
 * the original payload.
 * </p>
 * <p>Key behaviors include:</p>
 * <ul>
 * <li><b>Support:</b> Handles objects that satisfy {@code isCollection(payload)} or
 * {@code isArray(payload)}.</li>
 * <li><b>Type Preservation:</b> For arrays, it creates a new instance of the same
 * component type. For collections, it returns a new {@code ArrayList}.</li>
 * <li><b>Recursive Processing:</b> Automatically triggers {@code context.processMask()}
 * for non-primitive elements to ensure nested sensitive data is handled.</li>
 * <li><b>Circular Dependency Protection:</b> Registers the collection or array with
 * the {@link SensitiveMaskerContext} before processing elements to prevent infinite
 * recursion in self-referencing data structures.</li>
 * </ul>
 */
public class CollectionSensitiveMaskerStrategy implements SensitiveMaskerStrategy {

    @Override
    public boolean supports(final Object payload) {
        return isCollection(payload) || isArray(payload);
    }

    @Override
    public Object mask(final Object payload,
                       final SensitiveMaskerContext context) {
        if (isCollection(payload)) {
            return maskCollection((Collection<?>) payload, context);
        }
        if (isArray(payload)) {
            return maskArray(payload, context);
        }
        return payload;
    }

    private Object maskCollection(final Collection<?> payload,
                                  final SensitiveMaskerContext context) {
        final var proceedCollection = new ArrayList<>(payload.size());

        context.registerVisited(payload, proceedCollection);

        for (final var item : payload) {
            if (Objects.isNull(item) ||
                    isPrimitive(item.getClass())) {
                proceedCollection.add(item);
            } else {
                final var mask = context.processMask(item);
                proceedCollection.add(mask);
            }
        }

        return proceedCollection;
    }

    private Object maskArray(final Object payload,
                             final SensitiveMaskerContext context) {
        final var length = Array.getLength(payload);
        final var proceedArray = Array.newInstance(Object.class, length);

        context.registerVisited(payload, proceedArray);

        for (var idx = 0; idx < length; idx++) {
            final var item = Array.get(payload, idx);
            if (Objects.isNull(item) ||
                    isPrimitive(item.getClass())) {
                Array.set(proceedArray, idx, item);
            } else {
                final var mask = context.processMask(item);
                Array.set(proceedArray, idx, mask);
            }
        }
        return proceedArray;
    }
}

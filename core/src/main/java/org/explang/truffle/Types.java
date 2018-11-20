package org.explang.truffle;

import com.oracle.truffle.api.dsl.TypeSystem;

// Truffle type system declaration, though if everything is statically resolved
// this might not be needed.
@TypeSystem({double.class, ExplSymbol.class, ExplFunction.class})
public class Types {
}

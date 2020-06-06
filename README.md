# expl
An embeddable expression language for modelling and exploring.

This is a very early work in progress.

Goals:
- An accessible syntax to analysts and modellers, without feeling 
too much like "programming" (think spreadsheet formulae)
- Easily embeddable in applications and other programming environments
- Fast numerical calculation, vector/array manipulation, tabular data
- No efficiency penalty for natural expression of a solution
- Support for explorable interfaces and extreme debug-ability

Approach:
- Limited goals, not a full-featured application development language
- High level of abstraction enabling extreme compiler restructuring
- Purely functional, immutable, strictly-typed, but "blue-collar" pragmatism

For now, I'm exploring these ideas via an interpreter, but the goal is to JIT-compile.

Inspirations: Spreadsheets, Pandas, Julia, Wolfram, JQ

*Expl* is not a very good name. I'm looking for a new one.

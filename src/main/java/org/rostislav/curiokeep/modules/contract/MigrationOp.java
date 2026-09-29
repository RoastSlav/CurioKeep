package org.rostislav.curiokeep.modules.contract;

/** What a migration step does to an item's attributes. */
public enum MigrationOp {
    /** Copies the value of one field to another and removes the old key, when the target is empty. */
    MOVE,
    /** Like {@link #MOVE}, but the old value stays. */
    COPY,
    /** Replaces the value of a field according to a table; a value not in the table is left as it is. */
    MAP,
    /** Sets a value when the field has none. */
    DEFAULT,
    /** Removes a field's value. The only step that discards data, so it is always the module author's explicit choice. */
    DROP
}

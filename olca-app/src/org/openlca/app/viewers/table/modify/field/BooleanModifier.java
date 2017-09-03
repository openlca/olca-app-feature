package org.openlca.app.viewers.table.modify.field;

import java.util.Objects;
import java.util.function.Consumer;

import org.openlca.app.editors.IEditor;
import org.openlca.app.util.Bean;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BooleanModifier<T> extends CheckBoxCellModifier<T> {

	private static final Logger log = LoggerFactory.getLogger(BooleanModifier.class);
	private final String field;
	private final IEditor editor;
	private final Consumer<T> onChange;

	public BooleanModifier(String field) {
		this(null, field, null);
	}

	public BooleanModifier(IEditor editor, String field) {
		this(editor, field, null);
	}

	public BooleanModifier(String field, Consumer<T> onChange) {
		this(null, field, onChange);
	}

	public BooleanModifier(IEditor editor, String field, Consumer<T> onChange) {
		this.field = field;
		this.editor = editor;
		this.onChange = onChange;
	}

	@Override
	protected boolean isChecked(T element) {
		try {
			Object value = Bean.getValue(element, field);
			if (value == null)
				return false;
			if (!(value instanceof Boolean))
				return false;
			return (boolean) value;
		} catch (Exception e) {
			log.error("Error getting value from bean", e);
			return false;
		}
	}

	@Override
	protected void setChecked(T element, boolean value) {
		try {
			Object original = Bean.getValue(element, field);
			if (Objects.equals(original, value))
				return;
			Bean.setValue(element, field, value);
			if (editor != null)
				editor.setDirty(true);
			if (onChange != null)
				onChange.accept(element);
		} catch (Exception e) {
			log.error("Error setting value to bean", e);
		}
	}

}

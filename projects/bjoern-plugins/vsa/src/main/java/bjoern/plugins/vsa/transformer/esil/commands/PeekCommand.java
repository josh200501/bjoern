package bjoern.plugins.vsa.transformer.esil.commands;

import bjoern.plugins.vsa.domain.AbstractEnvironment;
import bjoern.plugins.vsa.domain.ValueSet;
import bjoern.plugins.vsa.domain.memrgn.LocalRegion;
import bjoern.plugins.vsa.domain.memrgn.MemoryRegion;
import bjoern.plugins.vsa.structures.DataWidth;
import bjoern.plugins.vsa.structures.StridedInterval;
import bjoern.plugins.vsa.transformer.esil.stack.ESILStackItem;
import bjoern.plugins.vsa.transformer.esil.stack.ValueSetContainer;

import java.util.Deque;

public class PeekCommand implements ESILCommand {

	@Override
	public ESILStackItem execute(
			Deque<ESILCommand> stack, AbstractEnvironment env) {
		ValueSet value = stack.pop().execute(stack, env).getValue();
		StridedInterval bpOffset = getValueOfLocalRegion(value);
		// Peek from non-local region.
		if (bpOffset.isBottom()) {
			return new ValueSetContainer(ValueSet.newTop(DataWidth.R64));
		}
		ValueSet tmp = env.getRegister("rbp");
		if (tmp != null) {
			StridedInterval rbp = getValueOfLocalRegion(tmp);
			if (rbp.isBottom()) {
				return new ValueSetContainer(ValueSet.newTop(DataWidth.R64));
			}
			StridedInterval spOffset = bpOffset.sub(rbp);
			if (spOffset.isSingletonSet()) {
				for (long address : spOffset.values()) {
					ValueSet data = env.getSPVariable(address);
					if (data != null) {
						return new ValueSetContainer(data);
					}
				}
			}
		}
		return new ValueSetContainer(ValueSet.newTop(DataWidth.R64));
	}

	private StridedInterval getValueOfLocalRegion(ValueSet valueSet) {
		if (valueSet.isTop()) {
			return StridedInterval.getTop(valueSet.getDataWidth());
		}
		for (MemoryRegion region : valueSet.getRegions()) {
			if (region instanceof LocalRegion) {
				StridedInterval interval = valueSet.getValueOfRegion(region);
				return interval;
			}
		}
		return StridedInterval.getBottom(valueSet.getDataWidth());
	}
}

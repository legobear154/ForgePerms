/*
 * PermissionsEx - Permissions plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package pex.permissions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 * @author t3hk0d3
 */
public abstract class PermissionGroup extends PermissionEntity implements Comparable<PermissionGroup> {

	protected final static String NON_INHERITABLE_PREFIX = "#";

	protected int weight = 0;
	protected boolean dirtyWeight = true;

	public PermissionGroup(String groupName, PermissionManager manager) {
		super(groupName, manager);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (isDebug()) {
			Logger.getLogger("Minecraft").info("[PermissionsEx] Group " + getName() + " initialized");
		}
	}

	/**
	 * Return non-inherited group prefix. This means if a group don't have has
	 * own prefix then empty string or null would be returned
	 * 
	 * @return prefix as string
	 */
	public String getOwnPrefix() {
		return this.getOwnPrefix(null);
	}

	public abstract String getOwnPrefix(String worldName);

	/**
	 * Return non-inherited suffix prefix. This means if a group don't has own
	 * suffix then empty string or null would be returned
	 * 
	 * @return suffix as string
	 */
	public final String getOwnSuffix() {
		return this.getOwnSuffix(null);
	}

	public abstract String getOwnSuffix(String worldName);

	/**
	 * Returns own (without inheritance) permissions of group for world
	 * 
	 * @param world
	 *            world's world name
	 * @return Array of permissions for world
	 */
	public abstract String[] getOwnPermissions(String world);

	/**
	 * Returns option value in specified world without inheritance This mean
	 * option value wouldn't be inherited from parent groups
	 * 
	 * @param option
	 * @param world
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found in own
	 *         options
	 */
	public abstract String getOwnOption(String option, String world, String defaultValue);

	public String getOwnOption(String option) {
		return this.getOwnOption(option, null, null);
	}

	public String getOwnOption(String option, String world) {
		return this.getOwnOption(option, world, null);
	}

	public boolean getOwnOptionBoolean(String optionName, String world, boolean defaultValue) {
		String option = this.getOwnOption(optionName, world, Boolean.toString(defaultValue));

		if ("false".equalsIgnoreCase(option)) {
			return false;
		} else if ("true".equalsIgnoreCase(option)) {
			return true;
		}

		return defaultValue;
	}

	public int getOwnOptionInteger(String optionName, String world, int defaultValue) {
		String option = this.getOwnOption(optionName, world, Integer.toString(defaultValue));

		try {
			return Integer.parseInt(option);
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	public double getOwnOptionDouble(String optionName, String world, double defaultValue) {
		String option = this.getOwnOption(optionName, world, Double.toString(defaultValue));

		try {
			return Double.parseDouble(option);
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	public int getWeight() {
		if (dirtyWeight) {
			weight = getOptionInteger("weight", null, 0);
			dirtyWeight = false;
		}

		return weight;
	}

	public void setWeight(int weight) {
		this.setOption("weight", Integer.toString(weight));

		dirtyWeight = true;

		clearMembersCache();
	}

	/**
	 * Checks if group is participating in ranking system
	 * 
	 * @return
	 */
	public boolean isRanked() {
		return getRank() > 0;
	}

	/**
	 * Returns rank in ranking system. 0 if group is not ranked
	 * 
	 * @return
	 */
	public int getRank() {
		return getOwnOptionInteger("rank", null, 0);
	}

	/**
	 * Set rank for this group
	 * 
	 * @param rank
	 *            Rank for group. Specify 0 to remove group from ranking
	 */
	public void setRank(int rank) {
		if (rank > 0) {
			this.setOption("rank", Integer.toString(rank));
		} else {
			this.setOption("rank", null);
		}
	}

	/**
	 * Returns ranking ladder where this group is participating in
	 * 
	 * @return Name of rank ladder as String
	 */
	public String getRankLadder() {
		return this.getOption("rank-ladder", "", "default");
	}

	/**
	 * Set rank ladder for this group
	 * 
	 * @param rankLadder
	 *            Name of rank ladder
	 */
	public void setRankLadder(String rankLadder) {
		if (rankLadder.isEmpty() || rankLadder.equals("default")) {
			rankLadder = null;
		}

		this.setOption("rank-ladder", rankLadder);
	}

	protected abstract String[] getParentGroupsNamesImpl(String worldName);

	/**
	 * Returns array of parent groups objects
	 * 
	 * @return array of groups objects
	 */
	public PermissionGroup[] getParentGroups(String worldName) {
		List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>();

		for (String parentGroup : getParentGroupsNamesImpl(worldName)) {

			// Yeah horrible thing, i know, that just safety from invoking empty
			// named groups
			parentGroup = parentGroup.trim();
			if (parentGroup.isEmpty()) {
				continue;
			}

			if (parentGroup.equals(getName())) {
				continue;
			}

			PermissionGroup group = manager.getGroup(parentGroup);
			if (!parentGroups.contains(group) && !group.isChildOf(this, worldName, true)) { // To
																							// prevent
																							// cyclic
																							// inheritance
				parentGroups.add(group);
			}
		}

		if (worldName != null) {
			// World Inheritance
			for (String parentWorld : manager.getWorldInheritance(worldName)) {
				parentGroups.addAll(Arrays.asList(getParentGroups(parentWorld)));
			}

			parentGroups.addAll(Arrays.asList(getParentGroups(null)));
		}

		Collections.sort(parentGroups);

		return parentGroups.toArray(new PermissionGroup[0]);
	}

	public PermissionGroup[] getParentGroups() {
		return this.getParentGroups(null);
	}

	public Map<String, PermissionGroup[]> getAllParentGroups() {
		Map<String, PermissionGroup[]> allGroups = new HashMap<String, PermissionGroup[]>();

		for (String worldName : getWorlds()) {
			allGroups.put(worldName, getWorldGroups(worldName));
		}

		allGroups.put(null, getWorldGroups(null));

		return allGroups;
	}

	protected PermissionGroup[] getWorldGroups(String worldName) {
		List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

		for (String groupName : getParentGroupsNamesImpl(worldName)) {
			if (groupName == null || groupName.isEmpty() || groupName.equalsIgnoreCase(getName())) {
				continue;
			}

			PermissionGroup group = manager.getGroup(groupName);

			if (!groups.contains(group)) {
				groups.add(group);
			}
		}

		Collections.sort(groups);

		return groups.toArray(new PermissionGroup[0]);
	}

	/**
	 * Returns direct parents names of this group
	 * 
	 * @return array of parents group names
	 */
	public String[] getParentGroupsNames(String worldName) {
		List<String> groups = new LinkedList<String>();
		for (PermissionGroup group : this.getParentGroups(worldName)) {
			groups.add(group.getName());
		}

		return groups.toArray(new String[0]);
	}

	public String[] getParentGroupsNames() {
		return this.getParentGroupsNames(null);
	}

	/**
	 * Set parent groups
	 * 
	 * @param parentGroups
	 *            Array of parent groups names to set
	 */
	public abstract void setParentGroups(String[] parentGroups, String worldName);

	public void setParentGroups(String[] parentGroups) {
		this.setParentGroups(parentGroups, null);
	}

	/**
	 * Set parent groups
	 * 
	 * @param parentGroups
	 *            Array of parent groups objects to set
	 */
	public void setParentGroups(PermissionGroup[] parentGroups, String worldName) {
		List<String> groups = new LinkedList<String>();

		for (PermissionGroup group : parentGroups) {
			groups.add(group.getName());
		}

		this.setParentGroups(groups.toArray(new String[0]), worldName);
	}

	public void setParentGroups(PermissionGroup[] parentGroups) {
		this.setParentGroups(parentGroups, null);
	}

	protected abstract void removeGroup();

	/**
	 * Check if this group is descendant of specified group
	 * 
	 * @param group
	 *            group object of parent
	 * @param checkInheritance
	 *            set to false to check only the direct inheritance
	 * @return true if this group is descendant or direct parent of specified
	 *         group
	 */
	public boolean isChildOf(PermissionGroup group, String worldName, boolean checkInheritance) {
		if (group == null) {
			return false;
		}

		for (PermissionGroup parentGroup : this.getParentGroups(worldName)) {
			if (group.equals(parentGroup)) {
				return true;
			}

			if (checkInheritance && parentGroup.isChildOf(group, worldName, checkInheritance)) {
				return true;
			}
		}

		return false;
	}

	public boolean isChildOf(PermissionGroup group, boolean checkInheritance) {
		for (String worldName : getWorlds()) {
			if (this.isChildOf(group, worldName, checkInheritance)) {
				return true;
			}
		}

		return this.isChildOf(group, null, checkInheritance);
	}

	public boolean isChildOf(PermissionGroup group, String worldName) {
		return isChildOf(group, worldName, false);
	}

	public boolean isChildOf(PermissionGroup group) {
		return isChildOf(group, false);
	}

	/**
	 * Check if this group is descendant of specified group
	 * 
	 * @param groupName
	 *            name of group to check against
	 * @param checkInheritance
	 *            set to false to check only the direct inheritance
	 * @return
	 */
	public boolean isChildOf(String groupName, String worldName, boolean checkInheritance) {
		return isChildOf(manager.getGroup(groupName), worldName, checkInheritance);
	}

	public boolean isChildOf(String groupName, boolean checkInheritance) {
		return isChildOf(manager.getGroup(groupName), checkInheritance);
	}

	/**
	 * Check if specified group is direct parent of this group
	 * 
	 * @param groupName
	 *            to check against
	 * @return
	 */
	public boolean isChildOf(String groupName, String worldName) {
		return this.isChildOf(groupName, worldName, false);
	}

	public boolean isChildOf(String groupName) {
		return this.isChildOf(groupName, false);
	}

	/**
	 * Return array of direct child group objects
	 * 
	 * @return
	 */
	public PermissionGroup[] getChildGroups(String worldName) {
		return manager.getGroups(getName(), worldName, false);
	}

	public PermissionGroup[] getChildGroups() {
		return manager.getGroups(getName(), false);
	}

	/**
	 * Return array of descendant group objects
	 * 
	 * @return
	 */
	public PermissionGroup[] getDescendantGroups(String worldName) {
		return manager.getGroups(getName(), worldName, true);
	}

	public PermissionGroup[] getDescendantGroups() {
		return manager.getGroups(getName(), true);
	}

	/**
	 * Return array of direct members (users) of this group
	 * 
	 * @return
	 */
	public PermissionUser[] getUsers(String worldName) {
		return manager.getUsers(getName(), worldName, false);
	}

	public PermissionUser[] getUsers() {
		return manager.getUsers(getName());
	}

	public boolean isDefault(String worldName) {
		return equals(manager.getDefaultGroup(worldName));
	}

	/**
	 * Overriden methods
	 */
	@Override
	public String getPrefix(String worldName) {
		// @TODO This method should be refactored

		String localPrefix = this.getOwnPrefix(worldName);

		if (worldName != null && (localPrefix == null || localPrefix.isEmpty())) {
			// World-inheritance
			for (String parentWorld : manager.getWorldInheritance(worldName)) {
				String prefix = this.getOwnPrefix(parentWorld);
				if (prefix != null && !prefix.isEmpty()) {
					localPrefix = prefix;
					break;
				}
			}

			// Common space
			if (localPrefix == null || localPrefix.isEmpty()) {
				localPrefix = this.getOwnPrefix(null);
			}
		}

		if (localPrefix == null || localPrefix.isEmpty()) {
			for (PermissionGroup group : this.getParentGroups(worldName)) {
				localPrefix = group.getPrefix(worldName);
				if (localPrefix != null && !localPrefix.isEmpty()) {
					break;
				}
			}
		}

		if (localPrefix == null) { // NPE safety
			localPrefix = "";
		}

		return localPrefix;
	}

	@Override
	public String getSuffix(String worldName) {
		// @TODO This method should be refactored

		String localSuffix = this.getOwnSuffix(worldName);

		if (worldName != null && (localSuffix == null || localSuffix.isEmpty())) {
			// World-inheritance
			for (String parentWorld : manager.getWorldInheritance(worldName)) {
				String suffix = this.getOwnSuffix(parentWorld);
				if (suffix != null && !suffix.isEmpty()) {
					localSuffix = suffix;
					break;
				}
			}

			// Common space
			if (localSuffix == null || localSuffix.isEmpty()) {
				localSuffix = this.getOwnSuffix(null);
			}
		}

		if (localSuffix == null || localSuffix.isEmpty()) {
			for (PermissionGroup group : this.getParentGroups(worldName)) {
				localSuffix = group.getSuffix(worldName);
				if (localSuffix != null && !localSuffix.isEmpty()) {
					break;
				}
			}
		}

		if (localSuffix == null) { // NPE safety
			localSuffix = "";
		}

		return localSuffix;
	}

	@Override
	public String[] getPermissions(String world) {
		List<String> permissions = new LinkedList<String>();
		getInheritedPermissions(world, permissions, true, false, true);
		return permissions.toArray(new String[0]);
	}

	@Override
	public void addPermission(String permission, String worldName) {
		List<String> permissions = new LinkedList<String>(Arrays.asList(getOwnPermissions(worldName)));

		if (permissions.contains(permission)) {
			permissions.remove(permission);
		}

		permissions.add(0, permission);

		this.setPermissions(permissions.toArray(new String[0]), worldName);
	}

	@Override
	public void removePermission(String permission, String worldName) {
		List<String> permissions = new LinkedList<String>(Arrays.asList(getOwnPermissions(worldName)));

		permissions.remove(permission);

		this.setPermissions(permissions.toArray(new String[0]), worldName);
	}

	protected void getInheritedPermissions(String worldName, List<String> permissions, boolean groupInheritance, boolean worldInheritance, boolean firstStep) {
		if (firstStep) {
			permissions.addAll(Arrays.asList(getTimedPermissions(worldName)));
			permissions.addAll(Arrays.asList(getOwnPermissions(worldName)));
		} else { // filter permissions for ancestors groups
			copyFilterPermissions(NON_INHERITABLE_PREFIX, permissions, getTimedPermissions(worldName));
			copyFilterPermissions(NON_INHERITABLE_PREFIX, permissions, getOwnPermissions(worldName));
		}

		if (worldName != null) {
			// World inheritance
			for (String parentWorld : manager.getWorldInheritance(worldName)) {
				getInheritedPermissions(parentWorld, permissions, false, true, firstStep);
			}
			// Common permission
			if (!worldInheritance) {
				getInheritedPermissions(null, permissions, false, true, firstStep);
			}
		}

		// Group inhertance
		if (groupInheritance) {
			for (PermissionGroup group : this.getParentGroups(worldName)) {
				group.getInheritedPermissions(worldName, permissions, true, false, false);
			}
		}
	}

	protected void copyFilterPermissions(String filterPrefix, List<String> to, String[] from) {
		for (String permission : from) {
			if (permission.startsWith(filterPrefix)) {
				continue;
			}
			to.add(permission);
		}
	}

	@Override
	public void addTimedPermission(String permission, String world, int lifeTime) {
		super.addTimedPermission(permission, world, lifeTime);

		clearMembersCache();
	}

	@Override
	public void removeTimedPermission(String permission, String world) {
		super.removeTimedPermission(permission, world);

		clearMembersCache();
	}

	protected void clearMembersCache() {
		for (PermissionUser user : this.getUsers()) {
			user.clearCache();
		}
	}

	@Override
	public final void remove() {
		for (String world : getWorlds()) {
			clearChildren(world);
		}

		clearChildren(null);

		removeGroup();
	}

	private void clearChildren(String worldName) {
		for (PermissionGroup group : this.getChildGroups(worldName)) {
			List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>(Arrays.asList(group.getParentGroups(worldName)));
			parentGroups.remove(this);

			group.setParentGroups(parentGroups.toArray(new PermissionGroup[0]), worldName);
		}

		for (PermissionUser user : this.getUsers(worldName)) {
			user.removeGroup(this, worldName);
		}
	}

	@Override
	public String getOption(String optionName, String worldName, String defaultValue) {
		String value = this.getOwnOption(optionName, worldName, null);
		if (value != null) {
			return value;
		}

		if (worldName != null) { // world inheritance
			for (String world : manager.getWorldInheritance(worldName)) {
				value = this.getOption(optionName, world, null);
				if (value != null) {
					return value;
				}
			}

			// Check common space
			value = this.getOption(optionName, null, null);
			if (value != null) {
				return value;
			}
		}

		// Inheritance
		for (PermissionGroup group : this.getParentGroups(worldName)) {
			value = group.getOption(optionName, worldName, null);
			if (value != null) {
				return value;
			}
		}

		// Nothing found
		return defaultValue;
	}

	@Override
	public int compareTo(PermissionGroup o) {
		return getWeight() - o.getWeight();
	}
}

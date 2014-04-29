/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package com.kurento.kmf.repository;

/**
 * This exception is thrown when the user is trying to create a repository item
 * with the same id than existing repository item.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * 
 */
public class DuplicateItemException extends RuntimeException {

	private static final long serialVersionUID = 3515920000618086477L;

	public DuplicateItemException(String id) {
		super("There is an item with id=" + id);
	}

}

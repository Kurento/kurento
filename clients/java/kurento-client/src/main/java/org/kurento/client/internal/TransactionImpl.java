/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.client.internal;

import java.util.ArrayList;
import java.util.List;

import org.kurento.client.Continuation;
import org.kurento.client.Transaction;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.client.operation.Operation;

public class TransactionImpl implements Transaction {

  private List<Operation> operations = new ArrayList<>();
  private RomManager manager;
  private int objectRef = 0;

  public TransactionImpl(RomManager manager) {
    this.manager = manager;
  }

  public void addOperation(Operation op) {
    this.operations.add(op);
  }

  @Override
  public void commit() {
    manager.transaction(operations);
  }

  @Override
  public void commit(Continuation<Void> continuation) {
    manager.transaction(operations, continuation);
  }

  public String nextObjectRef() {
    return "newref:" + objectRef++;
  }

  @Override
  public void rollback() {
    for (Operation op : operations) {
      op.rollback(null);
    }
  }
}

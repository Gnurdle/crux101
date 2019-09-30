# crux101

My playground for evaluating/torturing juxt's crux

My setup is a Intel(R) Core(TM) i7-4810MQ CPU @ 2.80GHz, with commodity SSD.

I'm using postgres for the document store, and rocks-db for the backend, all on the same box

## Usage

Single core.clj file with some testing

'stuff-it will load 50K records into the store, which is used to play later

there are a few tests - most of these are trying to tease out when crux is
using the indexes, by "feel"

## License

Copyright © 2019 Gnurdle Technology, LLC.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

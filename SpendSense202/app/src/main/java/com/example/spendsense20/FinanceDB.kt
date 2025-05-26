<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
/*package com.example.spendsense20
=======
package com.example.spendsense20
>>>>>>> 5591ece3884d05b8bdb7780a5870feabd4ac6446
=======
package com.example.spendsense20
>>>>>>> dbeb21d70aba9dc92381a0d1cc8edcdf1eb8cb3d
=======
package com.example.spendsense20
>>>>>>> dbeb21d70aba9dc92381a0d1cc8edcdf1eb8cb3d

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FinanceEntity::class], version = 1)
abstract class FinanceDB : RoomDatabase() {
    abstract fun FinanceDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDB? = null

        fun getDatabase(context: Context): FinanceDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDB::class.java,
                    "finances_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
*/
=======
>>>>>>> 5591ece3884d05b8bdb7780a5870feabd4ac6446
=======
>>>>>>> dbeb21d70aba9dc92381a0d1cc8edcdf1eb8cb3d
=======
>>>>>>> dbeb21d70aba9dc92381a0d1cc8edcdf1eb8cb3d

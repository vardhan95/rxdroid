package at.jclehner.rxdroid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragmentActivity;
//import com.google.gson.Gson;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.ui.DialogueLike;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.DateTime;

public class ImportActivity extends SherlockFragmentActivity
{
	private static final String TAG = ImportActivity.class.getSimpleName();

	class Dialogish extends DialogueLike
	{
		private ZipFile mZip;

		Dialogish() {
			setArguments(new Bundle());
		}

		@Override
		public void onResume()
		{
			super.onResume();

			setTitle(R.string._title_warning);

			try
			{
				mZip = new ZipFile(getIntent().getData().getSchemeSpecificPart());
				final Date cDate = new Date(Long.parseLong(mZip.getComment().split(":")[1]));

				setMessage(R.string._msg_restore_backup_warning, DateTime.toNativeDateAndTime(cDate));

				final String file = mZip.getFile().toString();
				final String extDir = Environment.getExternalStorageDirectory().toString();
				if(file.startsWith(extDir))
					setDetail(file.substring(extDir.length()));
				else
					setDetail(file);
			}
			catch(ZipException e)
			{
				handleZipException(e);
			}
		}

		@Override
		public void onButtonClick(View button, int which)
		{
			if(which == BUTTON_POSITIVE)
			{
				try
				{
					synchronized(Database.LOCK_DATA)
					{
						mZip.extractAll(RxDroid.getPackageInfo().applicationInfo.dataDir);
						Database.reload(getActivity());
						Settings.init(true);

						startActivity(getPackageManager().getLaunchIntentForPackage(RxDroid.getPackageInfo().packageName));
					}
				}
				catch(ZipException e)
				{
					handleZipException(e);
				}
			}

			finish();
		}

		private void handleZipException(ZipException e)
		{
			Log.w(TAG, e);

			setTitle(R.string._title_error);
			setMessage(R.string._msg_restore_backup_error, e.getLocalizedMessage());
			setDetail(mZip.getFile().toString());

			getButton(BUTTON_POSITIVE).setVisibility(View.GONE);
			setNegativeButtonText(android.R.string.ok);
		}
	}



	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, Components.NO_DATABASE_INIT);
		super.onCreate(savedInstanceState);

		getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
				new Dialogish()).commit();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}
}

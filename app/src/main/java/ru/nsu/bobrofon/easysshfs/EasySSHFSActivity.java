package ru.nsu.bobrofon.easysshfs;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

import ru.nsu.bobrofon.easysshfs.log.LogFragment;
import ru.nsu.bobrofon.easysshfs.mountpointList.mountpoint.EditFragment;
import ru.nsu.bobrofon.easysshfs.mountpointList.MountpointFragment;


public class EasySSHFSActivity extends ActionBarActivity
	implements NavigationDrawerFragment.NavigationDrawerCallbacks, MountpointFragment.OnFragmentInteractionListener {

	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;
	private Fragment[] mFragments;
	private static Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		new VersionUpdater(getApplicationContext()).update();

		mFragments = new Fragment[] {
			new MountpointFragment(),
			new LogFragment()
		};

		setContentView(R.layout.activity_easy_sshfs);

		mNavigationDrawerFragment = (NavigationDrawerFragment)
			getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(
			R.id.navigation_drawer,
			(DrawerLayout) findViewById(R.id.drawer_layout));

		((MountpointFragment)mFragments[0]).setDrawerStatus(mNavigationDrawerFragment);
		((LogFragment)mFragments[1]).setDrawerStatus(mNavigationDrawerFragment);
	}

	@Override
	public void onResume() {
		super.onResume();
		mContext = getApplicationContext();
	}

	@Override
	public void onPause() {
		super.onPause();
		mContext = null;
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		try {
			FragmentManager fragmentManager = getSupportFragmentManager();
			int backStackCount = fragmentManager.getBackStackEntryCount();
			while (backStackCount-- > 0) {
				fragmentManager.popBackStack();
			}

			fragmentManager.beginTransaction()
				.replace(R.id.container, mFragments[position])
				.commit();
		}
		catch (ArrayIndexOutOfBoundsException e) {
			finish();
		}
	}

	public void onSectionAttached(int titleId) {
		mTitle = getString(titleId);
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onFragmentInteraction(int id) {
		EditFragment editFragment = EditFragment.newInstance(id);
		editFragment.setDrawerStatus(mNavigationDrawerFragment);

		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
			.replace(R.id.container, editFragment)
			.addToBackStack(null)
			.commit();
	}

	public static void showToast(final CharSequence message) {
		if (mContext != null) {
			Toast toast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
			toast.show();
		}
	}

}

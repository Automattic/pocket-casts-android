Note: (A) means Automated Test. If automated UI tests were executed for the app, there's no need to run tests marked with (A) manually.

## Manual Tests

#### Account

- [ ] Create an account.
- [ ] Change email.
- [ ] Change password.
- [ ] Logout.
- [ ] Forgotten password.
- [ ] Login to account.
- [ ] Purchase Pocket Casts Plus.

#### Podcasts

- [ ] Subscribe to a new podcast in discover.
- [ ] Stream an episode.
- [ ] Download and play an episode.
- [ ] Open an episode card.
- [ ] Add an episode to Up Next.
- [ ] Mark an episode as played.
- [ ] Mark an episode as unplayed.
- [ ] Archive an episode.
- [ ] Unarchive an episode.
- [ ] Tap the episode card podcast link to open the podcast page.
- [ ] Open podcast settings.
- [ ] Search for an episode in the podcast.
- [ ] Unsubscribe from a podcast.
- [ ] Play a video episode e.g. Tech News Weekly (Video).
- [ ] Swipe an episode row to add to Up Next in first position.
- [ ] Swipe an episode row to add to Up Next in last position.

#### Folders

##### Folders: Create

- [ ] When creating a folder the search should filter the list of podcasts to add.
- [ ] When creating a folder check the sort works for ‘name’, ‘episode release date’ and ‘date added’.
- [ ] Create a folder and make sure the podcasts are only visible in that folder.
- [ ] Make sure the folder name can't be empty.
- [ ] After creating a folder you should be taken to the created folder page and not the root folder.
- [ ] The new folder’s sort type should default to the same as the home folder’s.

##### Folders: Edit

- [ ] Edit the folder name and colour.
- [ ] Make sure the folder name can't be empty.  
- [ ] Add and remove podcasts from an existing folder.
- [ ] Deleting a folder should prompt you for confirmation, and after deleting it all the podcasts from that folder should be back in the root folder.
- [ ] When in drag and drop mode, added podcasts should appear at the end of the folder.
- [ ] When the home folder is in drag and drop mode, removed podcasts should appear at the end of the home folder.

##### Folders: Podcasts Tab

- [ ] The folder artwork should show the podcasts in the sort order in the folder.
- [ ] When the home folder is sorted by drag & drop a created folder should be added to the top of the list.
- [ ] When the home folder is sorted by drag & drop subscribing to a podcast should be added to the bottom of the list.
- [ ] Change the home folder’s drag & drop positions and this shouldn’t effect a folder’s drag & drop positions.
- [ ] Sort the home folder by name. The podcast title and folder title should be ordered A-Z. The prefix of ‘The ‘ should be ignore. It should be case insensitive.
- [ ] Sort the home folder by episode release date. The home folder should be ordered from the latest episode released to the oldest. If an episode is archived or played it should be ignored. Folder podcasts should be included in this.
- [ ] Sort the home folder by the date added. The list should be ordered with the oldest added at the top. This includes the date the folder was added and not the podcasts in the folder.
- [ ] When badges are turned on folders should also display a sum badge of the folder’s podcast badges.
- [ ] Check the folder artwork design on the small grid, large grid and list layout.
- [ ] The podcast search should include a folder if the name matches. Tapping the folder should open the folder podcasts page.
- [ ] If the user doesn't have Pocket Casts Plus the folder shouldn't appear in the search results.

##### Folders: Open Folder Page

- [ ] The create folder icon only appears on the root folder page.
- [ ] The podcast search only appears on the root folder page.
- [ ] A folder shouldn’t contain a sub folder.
- [ ] A podcast can only be added to one folder.
- [ ] The sort in the folder should be separate from the home folder. Also different folders can have their own sort orders.
- [ ] Podcasts in folders should be able to be sorted by ‘Name’, ‘Episode Release Date’, ‘Date Added’, and ‘Drag and Drop’.

##### Folders: Podcast Page

- [ ] On the podcast page there should be a folder icon so the podcast can be added to a folder, or moved if it is already in a folder.
- [ ] Pocket Casts Plus
- [ ] You will only see folders if you are signed in to an account with a Pocket Casts Plus subscription.
- [ ] You will still see the create folder icon if you aren’t paying for a Plus subscription, tapping it will show an upgrade page.

#### Mini Player
- [ ] Play and pause an episode.
- [ ] Skip forward and back.
- [ ] Tap the Up Next icon to open the Up Next.
- [ ] Tap near the artwork to open the full screen player.
- [ ] Swipe down to close the full screen player.

#### Full Screen Player

- [ ] Play / pause an episode.
- [ ] Skip forward / backward.
- [ ] Change the time using the timeline scrubber.
- [ ] Open Playback Effects.
- [ ] Open Sleep Timer.
- [ ] Star an episode.
- [ ] Share an episode.
- [ ] Go to podcast.
- [ ] Rearrange action icons.
- [ ] Open show notes.
- [ ] Open Up Next.
- [ ] Play and rotate a video into landscape to make sure it goes into full screen.
- [ ] In landscape check when playing the video controls disappear after a timeout.
- [ ] Tap the video picture in picture button
- [ ] With picture in picture check the play, pause, skip and close buttons work.

#### Chromecast

- [ ] Play and pause episode through Chromecast.
- [ ] Skip forward / backward.
- [ ] Check when an episode finishes the next starts playing.

#### Up Next

- [ ] Rearrange Up Next episodes.
- [ ] Remove an episode.
- [ ] Remove multiple episodes using multi select.
- [ ] Clear the queue.
- [ ] Sign in to your account, add episodes to your Up Next, sign out of your account, change the Up Next, sign back in and your local Up Next shouldn't change.
- [ ] Sign in to your account, add episodes to your Up Next, sign out of your account, clear your Up Next, sign back in and your server Up Next should be seen on your device.

#### Search

- [ ] Search for a podcast and subscribe to it.
- [ ] The podcasts tab search should return your subscribed local podcasts first. The subscribed podcasts should be sorted A to Z. After these should be the server search results.  
- [ ] The discover tab search should not return your local podcasts first.

#### Podcast Grid

- [ ] Turn badges on and off.
- [ ] Change the layout.
- [ ] Change the sort type.
- [ ] Drag and drop podcasts to a new position.
- [ ] Share a podcast list.

#### Podcast shares

- [ ] Open a podcast share list.

#### Discover

- [ ] Tap show all on a list.
- [ ] Open a category list.
- [ ] Change your region.

#### Profile

- [ ] Open Stats.
- [ ] Open Downloads.
- [ ] Open Files.
- [ ] Open Starred.
- [ ] Open Listening History.

#### Settings

- [ ] Open up every section.
- [ ] Opml export / import.

#### Android Auto

- [ ] Press all the buttons.

#### Android Automotive

- [ ] Try the app in the Polestar emulator.
- [ ] Try the app in a landscape emulator.

#### Playback Notification

- [ ] Test the player controls work.
- [ ] Test the seek bar works.

#### Sonos

- [ ] Make sure you can link an account.
- [ ] Check your podcasts and filters lists.

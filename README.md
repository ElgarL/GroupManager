## GroupManager

GroupManager is a plug-in for Sigot/Bukkit based Minecraft servers. It allows you to group permissions together and add players to each group. Each group can have a full inheritance tree of other sub-groups, enabling players to have differing levels of authority and control with a promotion and demotion hierachy.

The most traditional system (shown below) uses group inheritance, starting with new players at the top, and owners on the bottom. On smaller servers you might end up merging the moderator and admin rolls, but most people will have different opinions when it comes to which powers the admin should have access to, and which powers moderators should receive as well.

| **Newbie** |
|	The newbie group contains the very basic permission nodes, and should be safe to give all newly |
|	joined players upon login. |
| **Player** |
|	The player group covers all the commands that established players can use. This usually includes |
|	commands which involve the economy, basic teleportation, and other basic commands. |
| **Moderator** |
|	The next two groups can be merged or split even further based on your preference. Typically the |
|	lowest moderator rank would have access to kick users, and jail a user, while higher ranks |
|	could have access to IP ban players, turn invisible, and spawn creative mode items. |
| **Admin** |
| **Owner** |
|	The owner group usually has access to all permissions/commands, and is usually the only one with |
|	commands that control plugins, define groups, and ability to shut down/restart the server. |
| --- |

----------------------------------------------------------------------------------------------
## Welcome to GitHub Pages
 
You can use the [editor on GitHub](https://github.com/ElgarL/GroupManager/edit/gh-pages/README.md) to maintain and preview the content for your website in Markdown files.

Whenever you commit to this repository, GitHub Pages will run [Jekyll](https://jekyllrb.com/) to rebuild the pages in your site, from the content in your Markdown files.

### Markdown

Markdown is a lightweight and easy-to-use syntax for styling your writing. It includes conventions for

```markdown
Syntax highlighted code block

# Header 1
## Header 2
### Header 3

- Bulleted
- List

1. Numbered
2. List

**Bold** and _Italic_ and `Code` text

[Link](url) and ![Image](src)
```

For more details see [GitHub Flavored Markdown](https://guides.github.com/features/mastering-markdown/).

### Jekyll Themes

Your Pages site will use the layout and styles from the Jekyll theme you have selected in your [repository settings](https://github.com/ElgarL/GroupManager/settings). The name of this theme is saved in the Jekyll `_config.yml` configuration file.

### Support or Contact

Having trouble with Pages? Check out our [documentation](https://help.github.com/categories/github-pages-basics/) or [contact support](https://github.com/contact) and we’ll help you sort it out.
